package org.videoApp.backend.auth;


public class CheckTokenRequest {
    private String jwt, test;


    public CheckTokenRequest(final String jwt, final String test) {
        this.jwt = jwt;
        this.test = test;
    }

    public String getJwt() {
        return jwt;
    }

    public String getTest() {
        return test;
    }
}