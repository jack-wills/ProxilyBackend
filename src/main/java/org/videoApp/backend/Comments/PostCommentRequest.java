package org.videoApp.backend.Comments;


public class PostCommentRequest {
    private String postID, jwt, content;

    public PostCommentRequest(final String postID, final String content, final String jwt) {
        this.postID = postID;
        this.content = content;
        this.jwt = jwt;
    }

    public String getPostID() {
        return postID;
    }

    public String getContent() {
        return content;
    }

    public String getJwt() {
        return jwt;
    }
}