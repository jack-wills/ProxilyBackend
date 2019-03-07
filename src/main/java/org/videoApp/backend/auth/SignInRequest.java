package org.videoApp.backend.auth;


public class SignInRequest {
    private String email, password;


    public SignInRequest(final String email, final String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}