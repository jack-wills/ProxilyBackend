package org.videoApp.backend.auth;


public class CheckTokenRequest {
    private String token;

    public CheckTokenRequest(final String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}