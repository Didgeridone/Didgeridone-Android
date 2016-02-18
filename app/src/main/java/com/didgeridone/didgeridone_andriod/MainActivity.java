package com.didgeridone.didgeridone_andriod;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.sample.geofencing.Constants;
import com.google.android.gms.location.sample.geofencing.GeofenceErrorMessages;
import com.google.android.gms.location.sample.geofencing.GeofenceTransitionsIntentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;


public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {

    private static final String TAG = MainActivity.class.getSimpleName();
    ArrayList<String> allTasks = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    HashMap<Integer, Object> mapper = new HashMap<Integer, Object>();

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * The list of geofences used in this sample.
     */
    protected ArrayList<Geofence> mGeofenceList;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = getSharedPreferences("userInfo", MODE_PRIVATE);
        final String userID = prefs.getString("userID", "default user id");
        System.out.println("USER ID HERE " + userID);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("Reminder_User_Id", userID);
                startActivity(intent);
            }
        });

        new DownloadTask().execute("https://didgeridone.herokuapp.com/task/" + userID);

        final ListView myList;
        myList = (ListView) findViewById(R.id.listView);

        myList.setClickable(true);
        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String obj = mapper.get((position)).toString();

                try {
                    JSONObject jsonObj = new JSONObject(obj);
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("Reminder_User_Id", userID);
                    intent.putExtra("Reminder_Task_Id", jsonObj.get("task_id").toString());
                    intent.putExtra("Reminder_Name", jsonObj.get("name").toString());
                    intent.putExtra("Reminder_Latitude", Double.parseDouble(jsonObj.get("lat").toString()));
                    intent.putExtra("Reminder_Longitude", Double.parseDouble(jsonObj.get("long").toString()));
                    intent.putExtra("Reminder_Radius", Double.parseDouble(jsonObj.get("radius").toString()));
                    startActivity(intent);

                } catch (Exception e) {
                    Log.d("Didgeridoo", "Exception", e);
                }
            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, allTasks);
        adapter.clear();
        adapter.notifyDataSetChanged();
        myList.setAdapter(adapter);

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);

        // Get the geofences used. Geofence data is hard coded in this sample.
        String testTaskID = "56c4eae7955ff711007353e8";
        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this geofence.
                .setRequestId("Remember to buy milk!")

                .setCircularRegion(
                        39.757731,
                        -105.00706799999999,
                        70f
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(60000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT |
                        Geofence.GEOFENCE_TRANSITION_DWELL)
                .build());

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        geofencesAddedButtonState(mGeofencesAdded);
    }

    public void geofencesEnableDisableButton(View view) {
        toggleGeofencesEnableDisable();
    }

    public void toggleGeofencesEnableDisable() {
        if (mGeofencesAdded == true) {
            removeGeofences();
        } else {
            addGeofences();
        }

        // Now update our service status
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
        geofencesAddedButtonState(mGeofencesAdded);
    }

    private void geofencesAddedButtonState(boolean state) {
        Button geofence_button = (Button) findViewById(R.id.button_geofence);

        if (state == true) {
            geofence_button.setBackgroundColor(Color.parseColor("#ffcc0000"));
        } else {
            geofence_button.setBackgroundColor(Color.parseColor("#ff669900"));
        }
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().

        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
//        geofencesAddedButtonState(mGeofencesAdded);
//        System.out.println("State Start: " + mGeofencesAdded);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {
                return downloadContent(params[0]);
            } catch (IOException e) {
                return "Unable to retrieve data. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            adapter.notifyDataSetChanged();
        }
    }

    private String downloadContent(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "HTTP Response: " + response);
            is = conn.getInputStream();
            String contentAsString = convertStreamToString(is);

            try {
                // Parse the entire JSON string
                JSONObject root = new JSONObject(contentAsString);
                JSONObject user = root.getJSONObject("user");
                JSONArray tasks = user.getJSONArray("tasks");

                for(int i=0;i<tasks.length();i++) {
                    // parse the JSON object into fields and values
                    JSONObject jsonTasks = tasks.getJSONObject(i);
                    String name = jsonTasks.getString("name");
                    allTasks.add(name);
                    int position = allTasks.indexOf(name);
                    mapper.put(position, jsonTasks);
                }

            } catch (Exception e) {
                Log.d("Didgeridoo","Exception",e);
            }

            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        Log.i(TAG, "Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {

        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.apply();

            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
//            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

}
