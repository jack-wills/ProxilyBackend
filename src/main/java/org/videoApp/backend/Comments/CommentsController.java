package org.videoApp.backend.Comments;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.TokenClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
public class CommentsController {

    private static final Logger LOG = LoggerFactory.getLogger(CommentsController.class);

    @Autowired
    private SQLClient sqlClient;

    Gson GSON = new Gson();

    @RequestMapping("/getComments")
    public String getComments(@RequestBody GetCommentsRequest request) {
        String sqlCommand = "SELECT comments.*, users.FirstName, users.LastName FROM comments\n INNER JOIN users ON comments.UserID = users.UserID\n WHERE PostID=?\n ORDER BY Votes*0.7 + (1/(NOW() - Timestamp))*0.3 DESC;";
        JSONArray values = new JSONArray();
        values.put(request.getPostID());
        JSONObject sqlOutput = sqlClient.getRows(sqlCommand, values);
        try {
            if (sqlOutput.has("error")) {
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            CommentItem[] outputArray = new CommentItem[sqlArray.length()];
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
                int userVote;
                sqlCommand = "SELECT * FROM comments_votes WHERE (UserID=? AND CommentID=?)";
                values = new JSONArray();
                values.put(claims.getBody().getSubject());
                values.put(item.getString("CommentID"));
                JSONObject userVoteQueryJson = sqlClient.getRow(sqlCommand, values);
                if (userVoteQueryJson.has("Vote")) {
                    if (userVoteQueryJson.getBoolean("Vote")) {
                        userVote = 1;
                    } else {
                        userVote = -1;
                    }
                } else {
                    userVote = 0;
                }
                outputArray[i] = new CommentItem(i + 1,
                        item.getString("Content"),
                        item.getString("FirstName") + " " + item.getString("LastName"),
                        item.getString("ProfilePicture"),
                        userVote,
                        item.getInt("Votes")-userVote,
                        item.getInt("CommentID"));
            }

            return GSON.toJson(outputArray);
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (IOException e) {
            LOG.error("IOException: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/postComment")
    public String postComment(@RequestBody PostCommentRequest request) {
        LocalDateTime ldt = LocalDateTime.now(Clock.systemUTC());
        DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("Content", request.getContent());
            sqlPutJson.put("UserID", claims.getBody().getSubject());
            sqlPutJson.put("Votes", 0);
            sqlPutJson.put("PostID", request.getPostID());
            sqlPutJson.put("Timestamp", formmat1.format(ldt));
            sqlClient.setRow(sqlPutJson, "comments", false);
            return "{\"success\": true}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException: {}", e);
            return "{\"error\": \"internal server error\"}";
        } catch (IOException e) {
            LOG.error("IOException: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/voteComment")
    public String voteComment(@RequestBody VoteCommentRequest request) {
        try {
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
            int previousVote;
            String sqlCommand = "SELECT Vote FROM comments_votes WHERE UserID=? AND CommentID=?;";
            JSONArray values = new JSONArray();
            values.put(claims.getBody().getSubject());
            values.put(request.getCommentID());
            JSONObject result = sqlClient.getRow(sqlCommand, values);
            if (result.has("error") && result.get("error").equals("OBJECT_NOT_FOUND")) {
                previousVote = 0;
            } else if (result.has("Vote")) {
                if (result.getBoolean("Vote")) {
                    previousVote = 1;
                } else {
                    previousVote = -1;
                }
            } else {
                return "{\"error\": \"" + result.get("error") + "\"}";
            }
            int voteDifference = request.getVote() - previousVote;
            sqlClient.executeCommand("UPDATE comments SET Votes = Votes + " + voteDifference + " WHERE CommentID='" + request.getCommentID() + "';");
            if (request.getVote() == 0) {
                sqlClient.executeCommand("DELETE FROM comments_votes WHERE UserID='" + claims.getBody().getSubject() + "' AND CommentID='" + request.getCommentID() + "';");
            } else {
                JSONObject json = new JSONObject();
                json.put("UserID", claims.getBody().getSubject());
                json.put("CommentID", request.getCommentID());
                if (request.getVote() == -1) {
                    json.put("Vote", 0);
                } else if (request.getVote() == 1) {
                    json.put("Vote", 1);
                } else {
                    return "{\"error\": \"Vote must be between -1 and 1\"}";
                }
                sqlClient.setRow(json, "comments_votes", true);
            }
            return "{\"success\": \"true\"}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (IOException e) {
            LOG.error("IOException: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }
}
