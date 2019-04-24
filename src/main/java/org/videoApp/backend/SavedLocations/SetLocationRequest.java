package org.videoApp.backend.SavedLocations;


public class SetLocationRequest {
    private float latitude, longitude;

    public SetLocationRequest(final float latitude, final float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

}