package org.videoApp.backend.auth;

public class RegisterFacebookAccountRequest {
    private String token;

    public RegisterFacebookAccountRequest(final String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
