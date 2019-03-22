package org.videoApp.backend.Comments;

public class CommentItem {
    private int id, userVote, totalVotes, commentId;
    private String submitter, comment, submitterProfilePicture;

    public CommentItem(final int id,
                       final String comment,
                       final String submitter,
                       final String submitterProfilePicture,
                       final int userVote,
                       final int totalVotes,
                       final int commentId) {
        this.id = id;
        this.comment = comment;
        this.submitter = submitter;
        this.submitterProfilePicture = submitterProfilePicture;
        this.userVote = userVote;
        this.totalVotes = totalVotes;
        this.commentId = commentId;
    }

    public int getId() {
        return id;
    }

    public String getComment() {
        return comment;
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

    public int getCommentId() {
        return commentId;
    }
}