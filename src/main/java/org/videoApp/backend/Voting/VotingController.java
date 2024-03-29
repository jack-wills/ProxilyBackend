package org.videoApp.backend.Voting;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.videoApp.backend.GetFeedItem.FeedItem;
import org.videoApp.backend.GetFeedItem.ImagePost;
import org.videoApp.backend.GetFeedItem.MediaPost;
import org.videoApp.backend.GetFeedItem.TextPost;
import org.videoApp.backend.GetFeedItem.VideoPost;
import org.videoApp.backend.ProxilyJwtFilter;
import org.videoApp.backend.SQLClient;

@RestController
public class VotingController {

    private static final Logger LOG = LoggerFactory.getLogger(VotingController.class);

    @Autowired
    private SQLClient sqlClient;

    @RequestMapping("/service/registerVote")
    public String registerVote(@RequestBody VotingRequest request, @RequestAttribute Jws<Claims> claims) {
        try {
            int previousVote;
            JSONArray values = new JSONArray();
            values.put(claims.getBody().getSubject());
            values.put(request.getPostID());
            JSONObject result = sqlClient.getRow("SELECT Vote FROM users_votes WHERE UserID=? AND PostID=?;", values);
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
            values = new JSONArray();
            values.put(voteDifference);
            values.put(request.getPostID());
            sqlClient.executeCommand("UPDATE posts SET Votes = Votes + ? WHERE PostID=?;", values);
            if (request.getVote() == 0) {
                values = new JSONArray();
                values.put(claims.getBody().getSubject());
                values.put(request.getPostID());
                sqlClient.executeCommand("DELETE FROM users_votes WHERE UserID=? AND PostID=?;", values);
            } else {
                JSONObject json = new JSONObject();
                json.put("UserID", claims.getBody().getSubject());
                json.put("PostID", request.getPostID());
                if (request.getVote() == -1) {
                    json.put("Vote", 0);
                } else if (request.getVote() == 1) {
                    json.put("Vote", 1);
                } else {
                    return "{\"error\": \"Vote must be between -1 and 1\"}";
                }
                sqlClient.setRow(json, "users_votes", true);
            }
            return "{\"success\": \"true\"}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping("/service/reportPost")
    public String reportPost(@RequestBody String requestString, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject request = new JSONObject(requestString);
            JSONObject json = new JSONObject();
            json.put("UserID", claims.getBody().getSubject());
            json.put("PostID", request.getString("postID"));
            sqlClient.setRow(json, "reported_posts", false);
            return "{\"success\": \"true\"}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping("/service/deletePost")
    public String deletePost(@RequestBody String requestString, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject request = new JSONObject(requestString);
            String sqlCommand =  "SELECT * FROM posts WHERE PostID=?";
            JSONArray values = new JSONArray();
            values.put(request.getString("postID"));
            JSONObject item = sqlClient.getRow(sqlCommand, values);
            if (item.has("error")) {
                return item.toString();
            }
            if (Integer.toString(item.getInt("UserID")).equals(claims.getBody().getSubject())) {
                values = new JSONArray();
                values.put(claims.getBody().getSubject());
                values.put(request.getString("postID"));
                sqlClient.executeCommand("DELETE FROM posts WHERE UserID=? AND PostID=?;", values);
                return "{\"success\": \"true\"}";
            }
            return "{\"error\": \"You are not authorised to perform this action\"}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

}
