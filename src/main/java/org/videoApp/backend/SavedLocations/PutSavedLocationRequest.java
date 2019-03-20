package org.videoApp.backend.SavedLocations;


public class PutSavedLocationRequest {
    private String jwt, name;
    private float latitude, longitude;

    public PutSavedLocationRequest(final float latitude, final float longitude, final String jwt, final String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.jwt = jwt;
        this.name = name;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public String getJwt() {
        return jwt;
    }

    public String getName() {
        return name;
    }
}