package org.videoApp.backend.Voting;


public class VotingRequest {
    private String postID, jwt;
    private int vote;

    public VotingRequest(final String postID, final int vote, final String jwt) {
        this.postID = postID;
        this.vote = vote;
        this.jwt = jwt;
    }

    public String getPostID() {
        return postID;
    }

    public int getVote() {
        return vote;
    }

    public String getJwt() {
        return jwt;
    }
}