package org.videoApp.backend.GetFeedItem;


public class FeedItem {
    private int id, userVote, totalVotes, postId;
    private String submitter, submitterProfilePicture;
    private MediaPost media;
    private boolean requestersPost;

    public FeedItem (final int id,
                     final MediaPost media,
                     final String submitter,
                     final String submitterProfilePicture,
                     final int userVote,
                     final int totalVotes,
                     final int postId,
                     final boolean requestersPost) {
        this.id = id;
        this.media = media;
        this.submitter = submitter;
        this.submitterProfilePicture = submitterProfilePicture;
        this.userVote = userVote;
        this.totalVotes = totalVotes;
        this.postId = postId;
        this.requestersPost = requestersPost;
    }

    public int getId() {
        return id;
    }

    public MediaPost getMedia() {
        return media;
    }

    public String getSubmitter() {
        return submitter;
    }

    public String getSubmitterProfilePicture() {
        return submitterProfilePicture;
    }

    public int getUserVote() {
        return userVote;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public int getPostId() {
        return postId;
    }

    public boolean isRequestersPost() {
        return requestersPost;
    }
}