package org.videoApp.backend.Voting;

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

@RestController
public class VotingController {

    private static final Logger LOG = LoggerFactory.getLogger(VotingController.class);

    @Autowired
    private SQLClient sqlClient;

    @RequestMapping("/registerVote")
    public String registerVote(@RequestBody VotingRequest request) {
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
