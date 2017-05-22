package com.traffic.trafficdemo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class GetRouteBtLocations extends IntentService {
    private ResultReceiver receiver;
    String google_maps_directions_key = "AIzaSyCABUpZpYTeSY8V1gC03oSJaaLcHOv9OFE";
    public static final String GOOGLE_DIRECTIONS = "https://maps.googleapis.com/maps/api/directions/json?";

    public GetRouteBtLocations() {
        super("GetRouteBtLocations");
    }

    public String getGoogleDirectionsUrl(String origin, String destination, Context context, String mode) {
        return GOOGLE_DIRECTIONS + "origin=" + origin + "&destination=" + destination + "&mode="+mode+"&departure_time=now&traffic_model=best_guess&key=" + google_maps_directions_key+"&alternatives=true";
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.receiver = intent.getParcelableExtra(Constants.KEY_RECEIVER);
        receiver.send(Constants.STATUS_RUNNING, Bundle.EMPTY);

        LocationEntity fromLocation = (LocationEntity) intent.getSerializableExtra(Constants.KEY_FROM);
        LocationEntity toLocation = (LocationEntity) intent.getSerializableExtra(Constants.KEY_TO);
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        try {
            String drivingDirectionUrl = getGoogleDirectionsUrl(fromLocation.latitude + "," +
                    fromLocation.longitude, toLocation.latitude + "," + toLocation.longitude, this, "driving");
            String walkingDirectionUrl = getGoogleDirectionsUrl(fromLocation.latitude + "," +
                    fromLocation.longitude, toLocation.latitude + "," + toLocation.longitude, this, "walking");
            ArrayList<RouteEntity> drivingRoutesList = getDirectionsList(drivingDirectionUrl, client, fromLocation, toLocation);
            ArrayList<RouteEntity> walkingRoutesList = getDirectionsList(walkingDirectionUrl, client, fromLocation, toLocation);

            Bundle bundle = new Bundle();
            bundle.putSerializable(Constants.KEY_DRIVING_DIRECTIONS_LIST, drivingRoutesList);
            bundle.putSerializable(Constants.KEY_WALKING_DIRECTIONS_LIST, walkingRoutesList);
            sendBundleData(Constants.STATUS_FINISHED, Constants.RESULT_SUCCESS, "No error", bundle);
        } catch (Exception ie) {
            ie.printStackTrace();
            sendBundleData(Constants.STATUS_ERROR, "Not Successful", "Error - " + ie.toString(), null);
        }
    }

    private ArrayList<RouteEntity> getDirectionsList(String url, OkHttpClient client, LocationEntity fromLocation, LocationEntity toLocation) throws Exception{
        JSONArray jRoutes = null, jLegs = null, jSteps = null;
        ArrayList<RouteEntity> routeEntityList = new ArrayList<RouteEntity>();
        String response = run(url, client);
        JSONObject mainObject = new JSONObject(response);
        jRoutes = mainObject.getJSONArray("routes");
        /** Traversing all routes */
        int routeCount;
        if (jRoutes.length() > 2)
            routeCount = 2;
        else
            routeCount = jRoutes.length();
        for (int i = 0; i < routeCount; i++) {
            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
            RouteEntity routeEntity = new RouteEntity();
            routeEntity.fromLocation = fromLocation;
            routeEntity.toLocation = toLocation;
            JSONObject routeObject = jRoutes.getJSONObject(i);
            JSONObject infoObject = routeObject.getJSONArray("legs").getJSONObject(0);
            JSONObject southWstObj = routeObject.getJSONObject("bounds").getJSONObject("southwest");

            LatLng southWest = new LatLng(southWstObj.getDouble("lat"), southWstObj.getDouble("lng"));

            JSONObject northEastObj = routeObject.getJSONObject("bounds").getJSONObject("northeast");
            LatLng northEast = new LatLng(northEastObj.getDouble("lat"), northEastObj.getDouble("lng"));

            LatLngBounds latLngBounds = new LatLngBounds(southWest, northEast);
            routeEntity.latLngBounds = latLngBounds;
            routeEntity.distance = infoObject.getJSONObject("distance").getString("text");
            routeEntity.time = infoObject.getJSONObject("duration").getString("text");
            if(infoObject.has("duration_in_traffic"))
                routeEntity.timeInTraffic = infoObject.getJSONObject("duration_in_traffic").getString("text");
            jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
            List<HashMap<String, String>> path = new ArrayList<HashMap<String, String>>();

            /** Traversing all legs */
            for (int j = 0; j < jLegs.length(); j++) {
                jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                /** Traversing all steps */
                for (int k = 0; k < jSteps.length(); k++) {
                    String polyline = "";
                    polyline = (String) ((JSONObject) ((JSONObject) jSteps
                            .get(k)).get("polyline")).get("points");
                    List<LatLng> list = decodePoly(polyline);

                    /** Traversing all points */
                    for (int l = 0; l < list.size(); l++) {
                        HashMap<String, String> hm = new HashMap<String, String>();
                        hm.put("lat",
                                Double.toString(list.get(l).latitude));
                        hm.put("lng",
                                Double.toString(list.get(l).longitude));
                        path.add(hm);
                    }
                }
                routes.add(path);
            }
            routeEntity.routes = routes;
            routeEntityList.add(routeEntity);
        }
        return routeEntityList;
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    String run(String url, OkHttpClient client) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private void sendBundleData(int resultCode, String successMsg, String error, Bundle resultBundle){
        Bundle bundle = new Bundle();
        bundle.putString(Constants.KEY_OUTPUT, successMsg);
        bundle.putBundle(Constants.KEY_BUNDLE_ITEM, resultBundle);
        bundle.putString(Constants.KEY_ERROR, error);
        receiver.send(resultCode, bundle);
    }

}
