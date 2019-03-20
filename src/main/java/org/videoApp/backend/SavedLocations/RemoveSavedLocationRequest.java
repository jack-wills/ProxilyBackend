package org.videoApp.backend.SavedLocations;


public class RemoveSavedLocationRequest {
    private String jwt;
    private int id;

    public RemoveSavedLocationRequest(final String jwt, final int id) {
        this.jwt = jwt;
        this.id = id;
    }

    public String getJwt() {
        return jwt;
    }

    public int getID() {
        return id;
    }
}