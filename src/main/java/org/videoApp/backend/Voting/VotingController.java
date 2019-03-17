package org.videoApp.backend.Voting;

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
import org.videoApp.backend.GetFeedItem.FeedItem;
import org.videoApp.backend.GetFeedItem.ImagePost;
import org.videoApp.backend.GetFeedItem.MediaPost;
import org.videoApp.backend.GetFeedItem.TextPost;
import org.videoApp.backend.GetFeedItem.VideoPost;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.TokenClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@RestController
public class VotingController {

    Gson GSON = new Gson();
    @RequestMapping("/registerVote")
    public String registerVote(@RequestBody VotingRequest request) {
        SQLClient sqlClient = new SQLClient();
        try {
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
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
            sqlClient.executeCommand("UPDATE posts SET Votes = Votes + " + voteDifference + " WHERE PostID='" + request.getPostID() + "';");
            if (request.getVote() == 0) {
                sqlClient.executeCommand("DELETE FROM users_votes WHERE UserID='" + claims.getBody().getSubject() + "' AND PostID='" + request.getPostID() + "';");
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
        } catch (IOException e) {
            return "{\"error\": \"internal server error\"}";
        }
    }

}
