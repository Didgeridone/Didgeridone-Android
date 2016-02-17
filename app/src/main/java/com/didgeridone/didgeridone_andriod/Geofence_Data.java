package com.didgeridone.didgeridone_andriod;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.android.wearable.geofencing.GeofenceTransitionsIntentService;
import com.example.android.wearable.geofencing.SimpleGeofence;
import com.example.android.wearable.geofencing.SimpleGeofenceStore;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import static com.example.android.wearable.geofencing.Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST;
import static com.example.android.wearable.geofencing.Constants.TAG;

/**
 * Created by jdrill on 2/17/16.
 */

public class Geofence_Data extends Application implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Internal List of Geofence objects. In a real app, these might be provided by an API based on
    // locations within the user's proximity.
    List<Geofence> mGeofenceList;

    // Persistent storage for geofences.
    private SimpleGeofenceStore mGeofenceStorage;

    private LocationServices mLocationService;
    // Stores the PendingIntent used to request geofence monitoring.
    private PendingIntent mGeofenceRequestIntent;
    private GoogleApiClient mApiClient;

    // Defines the allowable request types (in this example, we only add geofences).
    private enum REQUEST_TYPE {ADD}
    private REQUEST_TYPE mRequestType;

    public void Initiate_Geofence_Services() {
//        super.onCreate(savedInstanceState);
        // Rather than displayng this activity, simply display a toast indicating that the geofence
        // service is being created. This should happen in less than a second.
        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Google Play services unavailable.");
//            finish();
            return;
        }

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

        // Instantiate a new geofence storage area.
        mGeofenceStorage = new SimpleGeofenceStore(this);
        // Instantiate the current List of geofences.
        mGeofenceList = new ArrayList<Geofence>();
        createGeofences();
    }

    public void createGeofences() {
        // Create internal "flattened" objects containing the geofence data.
        SimpleGeofence mAndroidBuildingGeofence = new SimpleGeofence(
                "56c4ad756c11bc110089a24c",                // geofenceId.
                39.757591,
                -105.006579,
                10.0f,
                Geofence.NEVER_EXPIRE,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        );

        // Store these flat versions in SharedPreferences and add them to the geofence list.
        mGeofenceStorage.setGeofence("56c4ad756c11bc110089a24c", mAndroidBuildingGeofence);
        mGeofenceList.add(mAndroidBuildingGeofence.toGeofence());

        SimpleGeofence testGeo = mGeofenceStorage.getGeofence("56c4ad756c11bc110089a24c");
        System.out.println("*********************************Geo Test: " + testGeo.getTransitionType());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // If the error has a resolution, start a Google Play services activity to resolve it.
        if (connectionResult.hasResolution()) {
            System.out.println("***************************************************************");
            System.out.println("Commented code for checking startResolutionForResult");
            System.out.println("***************************************************************");
//            try {
//                connectionResult.startResolutionForResult(this,
//                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
//            } catch (IntentSender.SendIntentException e) {
//                Log.e(TAG, "Exception while resolving connection error.", e);
//            }
        } else {
            int errorCode = connectionResult.getErrorCode();
            Log.e(TAG, "Connection to Google Play services failed with error code " + errorCode);
        }
    }

    /**
     * Once the connection is available, send a request to add the Geofences.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Get the PendingIntent for the geofence monitoring request.
        // Send a request to add the current geofences.
        mGeofenceRequestIntent = getGeofenceTransitionPendingIntent();
        try {
//            LocationServices.GeofencingApi.addGeofences(mApiClient, mGeofenceList,
//                    mGeofenceRequestIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, getString(R.string.start_geofence_service), Toast.LENGTH_SHORT).show();
//        finish();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (null != mGeofenceRequestIntent) {
            LocationServices.GeofencingApi.removeGeofences(mApiClient, mGeofenceRequestIntent);
        }
    }


    /**
     * Checks if Google Play services is available.
     * @return true if it is.
     */
    private boolean isGooglePlayServicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Google Play services is available.");
            }
            return true;
        } else {
            Log.e(TAG, "Google Play services is unavailable.");
            return false;
        }
    }

    /**
     * Create a PendingIntent that triggers GeofenceTransitionIntentService when a geofence
     * transition occurs.
     */
    private PendingIntent getGeofenceTransitionPendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
