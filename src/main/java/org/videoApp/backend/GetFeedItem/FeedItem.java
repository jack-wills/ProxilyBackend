package org.videoApp.backend.GetFeedItem;


public class FeedItem {
    private int id, userVote, totalVotes, postId;
    private String submitter, submitterProfilePicture;
    private MediaPost media;

    public FeedItem (final int id,
                     final MediaPost media,
                     final String submitter,
                     final String submitterProfilePicture,
                     final int userVote,
                     final int totalVotes,
                     final int postId) {
        this.id = id;
        this.media = media;
        this.submitter = submitter;
        this.submitterProfilePicture = submitterProfilePicture;
        this.userVote = userVote;
        this.totalVotes = totalVotes;
        this.postId = postId;
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
}