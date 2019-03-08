package org.videoApp.backend.Comments;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.videoApp.backend.SQLClient;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
public class CommentsController {

    Gson GSON = new Gson();
    @RequestMapping("/getComments")
    public String getComments(@RequestBody GetCommentsRequest request) {
        SQLClient sqlClient = new SQLClient();
        String sqlCommand = "SELECT * FROM comments WHERE PostID=" + request.getPostID() + " ORDER BY Votes DESC";
        JSONObject sqlOutput = sqlClient.getRows(sqlCommand);
        try {
            if (sqlOutput.has("error")) {
                sqlClient.terminate();
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getJwt());
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            CommentItem[] outputArray = new CommentItem[sqlArray.length()];
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
                int userVote;
                JSONObject userVoteQueryJson = sqlClient.getRow("SELECT * FROM comments_votes WHERE (Email='" + claims.getBody().getSubject() + "' AND CommentID='" + item.getString("CommentID") + "')");
                if (userVoteQueryJson.has("Vote")) {
                    if (userVoteQueryJson.getBoolean("Vote")) {
                        userVote = 1;
                    } else {
                        userVote = -1;
                    }
                } else {
                    userVote = 0;
                }
                outputArray[i] = new CommentItem(i + 1, item.getString("Content"), item.getString("Submitter"), userVote, item.getInt("Votes"), item.getInt("CommentID"));
            }
            sqlClient.terminate();
            return GSON.toJson(outputArray);
        } catch (JSONException e) {
            sqlClient.terminate();
            System.out.println("JSONException: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            System.out.println("UnsupportedEncodingException: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping("/postComment")
    public String postComment(@RequestBody PostCommentRequest request) {
        SQLClient sqlClient = new SQLClient();
        LocalDateTime ldt = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getJwt());
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("Content", request.getContent());
            sqlPutJson.put("Submitter", claims.getBody().get("firstName") + " " + claims.getBody().get("lastName"));
            sqlPutJson.put("Votes", 0);
            sqlPutJson.put("PostID", request.getPostID());
            sqlPutJson.put("Timestamp", formmat1.format(ldt));
            sqlClient.setRow(sqlPutJson, "comments", false);
            sqlClient.terminate();
            return "{\"success\": true}";
        } catch (JSONException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/voteComment")
    public String voteComment(@RequestBody VoteCommentRequest request) {
        SQLClient sqlClient = new SQLClient();
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getJwt());
            int previousVote;
            JSONObject result = sqlClient.getRow("SELECT Vote FROM comments_votes WHERE Email='" + claims.getBody().getSubject() + "' AND CommentID='" + request.getCommentID() + "';");
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
                sqlClient.executeCommand("DELETE FROM comments_votes WHERE Email='" + claims.getBody().getSubject() + "' AND CommentID='" + request.getCommentID() + "';");
            } else {
                JSONObject json = new JSONObject();
                json.put("Email", claims.getBody().getSubject());
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
            sqlClient.terminate();
            return "{\"success\": \"true\"}";
        } catch (JSONException e) {
            sqlClient.terminate();
            System.out.println("JSONException: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            System.out.println("UnsupportedEncodingException: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
