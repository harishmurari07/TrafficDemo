package com.traffic.trafficdemo;

import com.google.android.gms.maps.model.LatLngBounds;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class RouteEntity implements Serializable {
    public LocationEntity fromLocation;
    public LocationEntity toLocation;
    public List<List<HashMap<String, String>>> routes;
    public String time;
    public String timeInTraffic;
    public String distance;
    public LatLngBounds latLngBounds;

}
