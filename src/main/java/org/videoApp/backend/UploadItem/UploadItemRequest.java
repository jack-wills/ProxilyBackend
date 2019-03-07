package org.videoApp.backend.UploadItem;


public class UploadItemRequest {
    private String latitude, longitude, jwt, mediaType, media;

    public UploadItemRequest(final String latitude, final String longitude, final String jwt, final String mediaType, final  String media) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.jwt = jwt;
        this.mediaType = mediaType;
        this.media = media;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getJwt() {
        return jwt;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getMedia() {
        return media;
    }
}