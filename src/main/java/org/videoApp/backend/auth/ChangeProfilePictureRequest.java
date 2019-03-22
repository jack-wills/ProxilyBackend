package org.videoApp.backend.auth;


public class ChangeProfilePictureRequest {
    private String token;

    public ChangeProfilePictureRequest(final String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}