package org.videoApp.backend.Voting;


public class ReportRequest {
    private String postID, jwt;

    public ReportRequest(final String postID, final String jwt) {
        this.postID = postID;
        this.jwt = jwt;
    }

    public String getPostID() {
        return postID;
    }

    public String getJwt() {
        return jwt;
    }
}