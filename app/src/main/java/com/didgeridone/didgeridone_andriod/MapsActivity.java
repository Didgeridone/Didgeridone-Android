package com.didgeridone.didgeridone_andriod;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

// To call this activity do this...
//Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
//intent.putExtra("Reminder_User_Id", "");
//intent.putExtra("Reminder_Task_Id", "");
//intent.putExtra("Reminder_Name", "Test Reminder Name");
//intent.putExtra("Reminder_Latitude", (double)39.75778308);
//intent.putExtra("Reminder_Longitude", (double)-105.00715055);
//intent.putExtra("Reminder_Radius", (double)12.0);
//startActivity(intent);

public class MapsActivity extends FragmentActivity implements OnMarkerDragListener,
        OnMapLongClickListener, OnMapReadyCallback {

    private String User_ID = "";
    private String Task_ID = "";
    private LatLng DEFAULT_LAT_LNG;
    private double DEFAULT_RADIUS = 12.0;
    private String Default_Name = "New Reminder";
    private static final String REMINDER_API_URL = "https://didgeridone.herokuapp.com/task/";
    public static final double RADIUS_OF_EARTH_METERS = 6371009;
    private static final int DEFAULT_CIRCLE_WIDTH = 5;
    private static final int DEFAULT_STROKE_COLOR = Color.BLACK;
    private static final int DEFAULT_FILL_COLOR = Color.HSVToColor(127, new float[]{0, 1, 1});
    private DraggableCircle Location_Circle;
    private GoogleMap mMap;
    private boolean Changes_Made = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get the passed in lat/lng from our intent
        Intent intent = getIntent();
        double Default_Lat = 0;
        double Default_Lng = 0;

        if (intent.getStringExtra("Reminder_Task_Id") == null){
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = lm.getProviders(true);
            Location l = null;

            for (int i = 0; i < providers.size(); i++) {
                try {
                    l = lm.getLastKnownLocation(providers.get(i));
                    if (l != null) {
                        Default_Lat = l.getLatitude();
                        Default_Lng = l.getLongitude();
                        break;
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Task_ID = intent.getStringExtra("Reminder_Task_Id");
        }

        User_ID = intent.getStringExtra("Reminder_User_Id");
        DEFAULT_LAT_LNG = new LatLng(intent.getDoubleExtra("Reminder_Latitude", Default_Lat),
                                     intent.getDoubleExtra("Reminder_Longitude", Default_Lng));
        DEFAULT_RADIUS = intent.getDoubleExtra("Reminder_Radius", DEFAULT_RADIUS);
        Default_Name = intent.getStringExtra("Reminder_Name");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Add the text watch listener to see if the user changes the reminder name
        EditText editText = (EditText) findViewById(R.id.editText_Reminder_Name);
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Enable_Save_Changes();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapLongClickListener(this);

        // Add a marker and move the camera
        Location_Circle = new DraggableCircle(DEFAULT_LAT_LNG, DEFAULT_RADIUS);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LAT_LNG, 19.0f));

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }

        EditText editText = (EditText) findViewById(R.id.editText_Reminder_Name);
        editText.setText(Default_Name, TextView.BufferType.EDITABLE);

        // Disable the save button by default
        Button save_button = (Button) findViewById(R.id.button_save);
        save_button.setEnabled(false);

        if (Task_ID == "") {
            // Disable the delete button since this is a new reminder
            Button delete_button = (Button) findViewById(R.id.button_delete);
            delete_button.setEnabled(false);
        }
    }

    private class DraggableCircle {

        private final Marker centerMarker;
        private final Marker radiusMarker;
        private final Circle circle;
        private double radius;

        public DraggableCircle(LatLng center, double radiusDouble) {
            this.radius = radiusDouble;
            centerMarker = mMap.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(true));
            radiusMarker = mMap.addMarker(new MarkerOptions()
                    .position(toRadiusLatLng(center, this.radius))
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE)));
            circle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(this.radius)
                    .strokeWidth(DEFAULT_CIRCLE_WIDTH)
                    .strokeColor(DEFAULT_STROKE_COLOR)
                    .fillColor(DEFAULT_FILL_COLOR));
        }

        public boolean onMarkerMoved(Marker marker) {
            if (marker.equals(centerMarker)) {
                circle.setCenter(marker.getPosition());
                radiusMarker.setPosition(toRadiusLatLng(marker.getPosition(), radius));
                return true;
            }
            if (marker.equals(radiusMarker)) {
                radius = toRadiusMeters(centerMarker.getPosition(), radiusMarker.getPosition());
                circle.setRadius(radius);
                return true;
            }
            return false;
        }
    }

    /** Generate LatLng of radius marker */
    private static LatLng toRadiusLatLng(LatLng center, double radius) {
        double radiusAngle = Math.toDegrees(radius / RADIUS_OF_EARTH_METERS) /
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    private static double toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        onMarkerMoved(marker);
    }

    private void onMarkerMoved(Marker marker) {
        Location_Circle.onMarkerMoved(marker);
        Enable_Save_Changes();
    }

    @Override
    public void onMapLongClick(LatLng point) {
        LatLng newCenter = new LatLng(point.latitude, point.longitude);
        Location_Circle.centerMarker.setPosition(newCenter);
        onMarkerMoved(Location_Circle.centerMarker);
    }

    public void Enable_Save_Changes() {
        Changes_Made = true;
        Button save_button = (Button) findViewById(R.id.button_save);
        save_button.setEnabled(true);
    }

    public void buttonBack(View v) {
        startActivity(new Intent(MapsActivity.this, MainActivity.class));
    }

    public void buttonDelete(View v) {

        new AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] Request_Array = {"DELETE", ""};
                        new Reminder_API().execute(Request_Array);
                        startActivity(new Intent(MapsActivity.this, MainActivity.class));
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void buttonSave(View v) {
        EditText editText = (EditText) findViewById(R.id.editText_Reminder_Name);

        // Reset errors and check the field
        editText.setError(null);

        if ( !isValidTaskName( editText.getText().toString() ) ) {
            editText.setError("Task name must be at least 4 characters.");
            editText.requestFocus();

        } else {

            try {
                JSONObject obj = new JSONObject();
                obj.put("name", editText.getText().toString());
                obj.put("lat", String.valueOf(Location_Circle.centerMarker.getPosition().latitude));
                obj.put("long", String.valueOf(Location_Circle.centerMarker.getPosition().longitude));
                obj.put("radius", Location_Circle.radius);
                obj.put("done", false);
                obj.put("enter", true);
                obj.put("task_id", Task_ID);

                // If our Task ID is empty we will add a new reminder
                if (Task_ID == "") {
                    String[] Request_Array = {"POST", obj.toString()};
                    new Reminder_API().execute(Request_Array);
                    //new Add_Reminder().execute(obj);
                } else {
                    String[] Request_Array = {"PUT", obj.toString()};
                    new Reminder_API().execute(Request_Array);
                    //new Update_Reminder().execute(obj);
                }

                startActivity(new Intent(MapsActivity.this, MainActivity.class));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class Reminder_API extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {

            String HTTP_Verb = params[0].toString();
            String HTTP_Data = params[1].toString();
            String HTTP_URL = "";

            if (HTTP_Verb == "POST") {
                HTTP_URL = REMINDER_API_URL + User_ID;
            } else if (HTTP_Verb == "PUT") {
                HTTP_URL = REMINDER_API_URL + User_ID + "/" + Task_ID;
            } else if (HTTP_Verb == "DELETE") {
                HTTP_URL = REMINDER_API_URL + User_ID + "/" + Task_ID;
            }

            try {
                URL url = new URL(HTTP_URL);
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod(HTTP_Verb);
                httpCon.setConnectTimeout(15000);
                httpCon.setRequestProperty("Content-type", "application/json");
                OutputStreamWriter out = new OutputStreamWriter( httpCon.getOutputStream() );
                out.write(HTTP_Data);
                out.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                System.out.println("********** RESPONSE:  " + sb.toString());

                httpCon.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            // Handle/Update UI Part
        }
    }

    public boolean isValidTaskName(String taskName) {
        return taskName.length() > 3;
    }
}
