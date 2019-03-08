CREATE TABLE comments (
    CommentID int NOT NULL AUTO_INCREMENT,
    Content varchar(255) NOT NULL,
    Submitter varchar(255) NOT NULL,
    Votes int,
    PostID int NOT NULL,
    Timestamp DATETIME,
    PRIMARY KEY (CommentID),
    FOREIGN KEY (PostID) REFERENCES posts(PostID)
);

CREATE TABLE comments_votes (
    CommentID int NOT NULL,
    Email varchar(255) NOT NULL,
    Vote bool,
    PRIMARY KEY (CommentID,Email),
    FOREIGN KEY (CommentID) REFERENCES comments(CommentID),
    FOREIGN KEY (Email) REFERENCES users(Email)
);