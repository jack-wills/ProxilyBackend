package org.videoApp.backend.Comments;


public class VoteCommentRequest {
    private String commentID, jwt;
    private int vote;

    public VoteCommentRequest(final String commenttID, final int vote, final String jwt) {
        this.commentID = commentID;
        this.vote = vote;
        this.jwt = jwt;
    }

    public String getCommentID() {
        return commentID;
    }

    public int getVote() {
        return vote;
    }

    public String getJwt() {
        return jwt;
    }
}