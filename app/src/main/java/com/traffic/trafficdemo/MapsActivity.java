package com.traffic.trafficdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
            ServiceResultReceiver.Receiver, View.OnClickListener{

    private GoogleMap mMap;
    Marker fromLocMarker, toLocMarker;
    Marker mainDrivingInfoMarker, altDrivingInfoMarker;
    Marker mainWalkingInfoMarker, altWalkingInfoMarker;
    Polyline mainDrivingPolyline, altDrivingPolyline;
    Polyline mainWalkingPolyline, altWalkingPolyline;
    AppCompatSpinner fromSpinner, toSpinner;
    Button getRouteButton;
    ServiceResultReceiver receiver;
    ArrayList<RouteEntity> drivingRoutesList = new ArrayList<>();
    ArrayList<RouteEntity> walkingRoutesList = new ArrayList<>();
    ArrayList<LocationEntity> fromLocationEntities = new ArrayList<>();
    ArrayList<LocationEntity> toLocationEntities = new ArrayList<>();
    ProgressDialog progressDialog;
    private boolean drivingModeEnabled = true;
    ImageButton modeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fromSpinner = (AppCompatSpinner) findViewById(R.id.fromSpinner);
        toSpinner = (AppCompatSpinner) findViewById(R.id.toSpinner);
        getRouteButton = (Button) findViewById(R.id.getRouteButton);
        modeButton = (ImageButton) findViewById(R.id.modeButton);
        mapFragment.getMapAsync(this);
        receiver = new ServiceResultReceiver(new Handler());
        receiver.setReceiver(this);
        initData();
        toggleModeIcon();
        getRouteButton.setOnClickListener(this);
        modeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.equals(getRouteButton)){
            getRouteDirections();
        }
        else if(view.equals(modeButton)){
            if(drivingRoutesList.size() == 0 || walkingRoutesList.size() == 0)
                return;
            toggleRoutes();
            toggleModeIcon();
        }
    }

    private void toggleRoutes(){
        if(drivingModeEnabled){
            drivingModeEnabled = false;
            loadRoutes();
        }
        else{
            drivingModeEnabled = true;
            loadRoutes();
        }
    }

    private void toggleModeIcon(){
        if(drivingModeEnabled){
            modeButton.setImageResource(R.drawable.ic_walking);
            showToast("Switching to Driving Mode");
        }
        else{
            modeButton.setImageResource(R.drawable.ic_driving);
            showToast("Switching to Walking Mode");
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo wifi = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        android.net.NetworkInfo mobile = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnected()) {
            return true;
        } else if (!mobile.isConnected()) {
            return false;
        } else if (mobile.isConnected()) {
            return true;
        }
        return false;
    }

    private void getRouteDirections(){
        if(!isInternetConnected()) {
            showToast("Sorry. Internet connection not available.");
            return;
        }
        Intent intent = new Intent(Constants.KEY_ACTION, null, this, GetRouteBtLocations.class);
        intent.putExtra(Constants.KEY_FROM, fromLocationEntities.get(fromSpinner.getSelectedItemPosition()));
        intent.putExtra(Constants.KEY_TO, toLocationEntities.get(toSpinner.getSelectedItemPosition()));
        intent.putExtra(Constants.KEY_RECEIVER, receiver);
        startService(intent);
    }

    private void initData(){
        fromLocationEntities.add(new LocationEntity("1369 Fulton St, Brooklyn, NY 11216, USA", 40.6804628,-73.9470251));
        fromLocationEntities.add(new LocationEntity("39 Centre St, New York, NY 10007, USA", 40.714113, -74.0037788));
        toLocationEntities.add(new LocationEntity("300 Jay St, Brooklyn, NY 11201, USA", 40.6955043, -73.9882616));
        toLocationEntities.add(new LocationEntity("1217 Park Ave, New York, NY 10128, USA", 40.785634, -73.9538753));
        fromSpinner.setAdapter(new LocationAdapter(this, R.layout.list_item, fromLocationEntities));
        toSpinner.setAdapter(new LocationAdapter(this, R.layout.list_item, toLocationEntities));
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
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case Constants.STATUS_RUNNING:
                showProgressDialog("Fetching Route...");
                break;
            case Constants.STATUS_FINISHED:
                dismissProgressDialog();
                String output = resultData.getString(Constants.KEY_OUTPUT);
                String error = resultData.getString(Constants.KEY_ERROR);
                if (output.equalsIgnoreCase(Constants.RESULT_SUCCESS)) {
                    showToast("Route Fetched");
                    Bundle bundle = resultData.getBundle(Constants.KEY_BUNDLE_ITEM);
                    drivingRoutesList = (ArrayList<RouteEntity>) bundle.getSerializable(Constants.KEY_DRIVING_DIRECTIONS_LIST);
                    walkingRoutesList = (ArrayList<RouteEntity>) bundle.getSerializable(Constants.KEY_WALKING_DIRECTIONS_LIST);
                    clearAll();
                    loadRoutes();
                }
                else
                    showToast(error);
                break;
            case Constants.STATUS_ERROR:
                dismissProgressDialog();
                String error1 = resultData.getString(Constants.KEY_ERROR);
                showToast(error1);
                break;
        }
    }


    private void showProgressDialog(String message){
        if(progressDialog == null)
            progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void dismissProgressDialog(){
        if(progressDialog != null)
            if(progressDialog.isShowing())
                progressDialog.dismiss();
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void clearAll(){
        if(mMap != null)
            mMap.clear();
        fromLocMarker = null;
        toLocMarker = null;
        mainDrivingInfoMarker = null;
        altDrivingInfoMarker = null;
        mainWalkingInfoMarker = null;
        altWalkingInfoMarker = null;
        mainDrivingPolyline = null;
        altDrivingPolyline = null;
        mainWalkingPolyline = null;
        altWalkingPolyline = null;
    }

    private void loadRoutes(){
        if(drivingModeEnabled) {
            if(mainDrivingPolyline == null || altDrivingPolyline == null) {
                if (drivingRoutesList != null) {
                    setMarker(drivingRoutesList.get(0).fromLocation, true, true);
                    setMarker(drivingRoutesList.get(0).toLocation, false, false);
                    if (drivingRoutesList.size() > 1)
                        drawRouteOnMap(drivingRoutesList.get(1), drivingRoutesList.get(1).routes, false);
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawRouteOnMap(drivingRoutesList.get(0), drivingRoutesList.get(0).routes, true);
                        }
                    }, 500);
                } else {
                    showToast("Routes are NULL");
                }
            }
            else{
                setMarker(drivingRoutesList.get(0).fromLocation, true, true);
                setMarker(drivingRoutesList.get(0).toLocation, false, false);
                if(mainDrivingPolyline != null)
                    mainDrivingPolyline.setVisible(true);
                if(altDrivingPolyline != null)
                    altDrivingPolyline.setVisible(true);
                if(mainDrivingInfoMarker != null)
                    mainDrivingInfoMarker.setVisible(true);
                if(altDrivingInfoMarker != null)
                    altDrivingInfoMarker.setVisible(true);

            }

            if(mainWalkingPolyline != null)
                mainWalkingPolyline.setVisible(false);
            if(altWalkingPolyline != null)
                altWalkingPolyline.setVisible(false);
            if(mainWalkingInfoMarker != null)
                mainWalkingInfoMarker.setVisible(false);
            if(altWalkingInfoMarker != null)
                altWalkingInfoMarker.setVisible(false);
        }
        else{
            if(mainWalkingPolyline == null || altWalkingPolyline == null) {
                if (walkingRoutesList != null) {
                    setMarker(walkingRoutesList.get(0).fromLocation, true, true);
                    setMarker(walkingRoutesList.get(0).toLocation, false, false);
                    if (walkingRoutesList.size() > 1)
                        drawRouteOnMap(walkingRoutesList.get(1), walkingRoutesList.get(1).routes, false);
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawRouteOnMap(walkingRoutesList.get(0), walkingRoutesList.get(0).routes, true);
                        }
                    }, 500);
                } else {
                    showToast("Routes are NULL");
                }
            }
            else{
                setMarker(walkingRoutesList.get(0).fromLocation, true, true);
                setMarker(walkingRoutesList.get(0).toLocation, false, false);

                if(mainWalkingPolyline != null)
                    mainWalkingPolyline.setVisible(true);
                if(altWalkingPolyline != null)
                    altWalkingPolyline.setVisible(true);
                if(mainWalkingInfoMarker != null)
                    mainWalkingInfoMarker.setVisible(true);
                if(altWalkingInfoMarker != null)
                    altWalkingInfoMarker.setVisible(true);
            }
            if(mainDrivingPolyline != null)
                mainDrivingPolyline.setVisible(false);
            if(altDrivingPolyline != null)
                altDrivingPolyline.setVisible(false);
            if(mainDrivingInfoMarker != null)
                mainDrivingInfoMarker.setVisible(false);
            if(altDrivingInfoMarker != null)
                altDrivingInfoMarker.setVisible(false);
        }
    }

    public void setMarker(LocationEntity location, boolean isFromLocation, boolean isZoom) {
        LatLng currentLatLng = new LatLng(location.latitude, location.longitude);
        BitmapDescriptor icon;
        if (isFromLocation) {
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            if(fromLocMarker != null)
                fromLocMarker.remove();
            fromLocMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .icon(icon));

        } else {
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            if(toLocMarker != null)
                toLocMarker.remove();
            toLocMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .icon(icon));
        }
    }

    public void drawRouteOnMap(final RouteEntity routeEntity, final List<List<HashMap<String, String>>> routes, final boolean isMainRoute) {
        if (routes != null) {
            final android.os.Handler handler = new android.os.Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<LatLng> points = new ArrayList<LatLng>();
                    final PolylineOptions polyLineOptions = new PolylineOptions();
                    // traversing through routes
                    for (int i = 0; i < routes.size(); i++) {
                        List<HashMap<String, String>> path = routes.get(i);
                        for (int j = 0; j < path.size(); j++) {
                            HashMap<String, String> point = path.get(j);
                            double lat = Double.parseDouble(point.get("lat"));
                            double lng = Double.parseDouble(point.get("lng"));
                            LatLng position = new LatLng(lat, lng);
                            points.add(position);
                        }
                        polyLineOptions.addAll(points);
                        polyLineOptions.width(15);
                        if (isMainRoute)
                            polyLineOptions.color(Color.BLUE);
                        else
                            polyLineOptions.color(Color.GRAY);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(drivingModeEnabled) {
                                    if (isMainRoute)
                                        mainDrivingPolyline = mMap.addPolyline(polyLineOptions);
                                    else
                                        altDrivingPolyline = mMap.addPolyline(polyLineOptions);
                                    if (routeEntity != null)
                                        addTimeandDistanceMarkerOnRoute(points.get(Math.round(points.size() / 2)), routeEntity, isMainRoute);
                                }
                                else{
                                    if (isMainRoute)
                                        mainWalkingPolyline = mMap.addPolyline(polyLineOptions);
                                    else
                                        altWalkingPolyline = mMap.addPolyline(polyLineOptions);
                                    if (routeEntity != null)
                                        addTimeandDistanceMarkerOnRoute(points.get(Math.round(points.size() / 2)), routeEntity, isMainRoute);
                                }

                            }
                        });


                    }
                }
            }).start();
            if(isMainRoute)
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(routeEntity.latLngBounds, 70));

        }
    }

    private void addTimeandDistanceMarkerOnRoute(LatLng position, RouteEntity routeEntity, boolean isMainRoute) {
        if(drivingModeEnabled) {
            if(isMainRoute) {
                if(mainDrivingInfoMarker == null) {
                    Bitmap bm = getBitmap(routeEntity);
                    mainDrivingInfoMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(BitmapDescriptorFactory.fromBitmap(bm)));
                }
            }
            else{
                if(altDrivingInfoMarker == null) {
                    Bitmap bm = getBitmap(routeEntity);
                    altDrivingInfoMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(BitmapDescriptorFactory.fromBitmap(bm)));
                }
            }
        }
        else{
            if(isMainRoute) {
                if(mainWalkingInfoMarker == null) {
                    Bitmap bm = getBitmap(routeEntity);
                    mainWalkingInfoMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(BitmapDescriptorFactory.fromBitmap(bm)));
                }
            }
            else{
                if(altWalkingInfoMarker == null) {
                    Bitmap bm = getBitmap(routeEntity);
                    altWalkingInfoMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(BitmapDescriptorFactory.fromBitmap(bm)));
                }
            }
        }
    }

    private Bitmap getBitmap(RouteEntity routeEntity){
        RelativeLayout tv = (RelativeLayout) getLayoutInflater().inflate(R.layout.map_window, null, false);
        tv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());
        TextView route_time = (TextView) tv.findViewById(R.id.route_time);
        TextView route_time_in_traffic = (TextView) tv.findViewById(R.id.route_time_in_traffic);
        TextView route_distance = (TextView) tv.findViewById(R.id.route_distance);
        route_time.setText(routeEntity.time);
        if(TextUtils.isEmpty(routeEntity.timeInTraffic))
            route_time_in_traffic.setVisibility(View.GONE);
        else
            route_time_in_traffic.setText(routeEntity.timeInTraffic+" (in traffic)");
        route_distance.setText(routeEntity.distance);
        tv.setDrawingCacheEnabled(true);
        tv.buildDrawingCache();
        Bitmap bm = tv.getDrawingCache();
        return bm;
    }

}
