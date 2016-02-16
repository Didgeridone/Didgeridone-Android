package com.didgeridone.didgeridone_andriod;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

// To call this activity do this...
//Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
//        intent.putExtra("Reminder_Name", "Test Reminder Name");
//        intent.putExtra("Reminder_Latitude", (double)39.75778308);
//        intent.putExtra("Reminder_Longitude", (double)-105.00715055);
//        intent.putExtra("Reminder_Radius", (double)12.0);
//        startActivity(intent);

public class MapsActivity extends FragmentActivity implements OnMarkerDragListener,
        OnMapLongClickListener, OnMapReadyCallback {

    private LatLng DEFAULT_LAT_LNG;
    private double DEFAULT_RADIUS = 12.0;
    private String Default_Name = "New Reminder";
    public static final double RADIUS_OF_EARTH_METERS = 6371009;
    private static final int DEFAULT_CIRCLE_WIDTH = 5;
    private static final int DEFAULT_STROKE_COLOR = Color.BLACK;
    private static final int DEFAULT_FILL_COLOR = Color.HSVToColor(127, new float[]{0, 1, 1});
    private List<DraggableCircle> mCircles = new ArrayList<DraggableCircle>(1);
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get the passed in lat/lng from our intent
        Intent intent = getIntent();
        DEFAULT_LAT_LNG = new LatLng(intent.getDoubleExtra("Reminder_Latitude", 39.75778308),
                                     intent.getDoubleExtra("Reminder_Longitude", -105.00715055));
        DEFAULT_RADIUS = intent.getDoubleExtra("Reminder_Radius", DEFAULT_RADIUS);
        Default_Name = intent.getStringExtra("Reminder_Name");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        DraggableCircle circle = new DraggableCircle(DEFAULT_LAT_LNG, DEFAULT_RADIUS);
        mCircles.add(circle);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LAT_LNG, 19.0f));
        mMap.setMyLocationEnabled(true);

        EditText editText = (EditText)findViewById(R.id.editText_Reminder_Name);
        editText.setText(Default_Name, TextView.BufferType.EDITABLE);
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
        for (DraggableCircle draggableCircle : mCircles) {
            if (draggableCircle.onMarkerMoved(marker)) {
                break;
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng point) {
        DraggableCircle circle = new DraggableCircle(point, DEFAULT_RADIUS);
        mCircles.add(circle);
    }
}
