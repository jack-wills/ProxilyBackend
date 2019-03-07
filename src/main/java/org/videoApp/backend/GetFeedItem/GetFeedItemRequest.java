package org.videoApp.backend.GetFeedItem;


public class GetFeedItemRequest {
    private String latitude, longitude, getPostsFrom, getPostsTo, jwt;

    public GetFeedItemRequest(final String latitude, final String longitude, final String getPostsFrom, final String getPostsTo, final String jwt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.getPostsFrom = getPostsFrom;
        this.getPostsTo = getPostsTo;
        this.jwt = jwt;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getGetPostsFrom() {
        return getPostsFrom;
    }

    public String getGetPostsTo() {
        return getPostsTo;
    }

    public String getJwt() {
        return jwt;
    }
}