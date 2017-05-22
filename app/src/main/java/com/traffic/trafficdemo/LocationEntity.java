package com.traffic.trafficdemo;

import java.io.Serializable;


public class LocationEntity implements Serializable {

    public double latitude;
    public double longitude;
    public String address;

    public LocationEntity(){

    }

    public LocationEntity(String address, double latitude, double longitude){
        this.address = address; this.latitude = latitude; this.longitude = longitude;
    }

}
