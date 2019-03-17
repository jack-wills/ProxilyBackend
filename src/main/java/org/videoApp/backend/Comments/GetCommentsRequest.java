package org.videoApp.backend.Comments;


public class GetCommentsRequest {
    private String jwt;
    private int postID;

    public GetCommentsRequest(final int postID, final String jwt) {
        this.postID = postID;
        this.jwt = jwt;
    }

    public int getPostID() {
        return postID;
    }

    public String getJwt() {
        return jwt;
    }
}