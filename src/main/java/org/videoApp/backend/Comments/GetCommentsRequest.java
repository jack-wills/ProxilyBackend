package org.videoApp.backend.Comments;


public class GetCommentsRequest {
    private String postID, jwt;

    public GetCommentsRequest(final String postID, final String jwt) {
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