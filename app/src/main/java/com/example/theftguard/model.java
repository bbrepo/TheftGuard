package com.example.theftguard;

public class model {
    String latitude,longitude,time,purl;
    model()
    {

    }
    public model(String latitude, String longitude, String email, String purl) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.purl = purl;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }
}
