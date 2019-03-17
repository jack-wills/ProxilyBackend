package org.videoApp.backend.GetFeedItem;


public class GetFeedItemRequest {
    private String jwt;
    private float latitude, longitude;
    private int getPostsFrom, getPostsTo;

    public GetFeedItemRequest(final float latitude, final float longitude, final int getPostsFrom, final int getPostsTo, final String jwt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.getPostsFrom = getPostsFrom;
        this.getPostsTo = getPostsTo;
        this.jwt = jwt;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public int getGetPostsFrom() {
        return getPostsFrom;
    }

    public int getGetPostsTo() {
        return getPostsTo;
    }

    public String getJwt() {
        return jwt;
    }
}