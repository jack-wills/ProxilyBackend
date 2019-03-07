package org.videoApp.backend.GetFeedItem;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.videoApp.backend.SQLClient;

import java.io.UnsupportedEncodingException;

@RestController
public class GetFeedController {

    Gson GSON = new Gson();
    //TODO Timestamp
    //Todo feed update (Require begging in request)
    @RequestMapping("/getPopularFeedItems")
    public String getPopularFeedItems(@RequestBody GetFeedItemRequest request) {
        SQLClient sqlClient = new SQLClient();
        String sqlCommand = getSQLQuery(request.getLatitude(), request.getLongitude(), "Votes DESC", request.getGetPostsFrom(), request.getGetPostsTo());
        System.out.println(sqlCommand);
        JSONObject sqlOutput = sqlClient.getRows(sqlCommand);
        if (sqlOutput.has("error")) {
            sqlClient.terminate();
            return sqlOutput.toString();
        }
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getJwt());
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            FeedItem[] outputArray = new FeedItem[sqlArray.length()];
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
                int userVote;
                JSONObject userVoteQueryJson = sqlClient.getRow("SELECT * FROM users_votes WHERE (Email = '" + claims.getBody().getSubject() + "' AND PostID = '" + item.getString("PostID") + "')");
                if (userVoteQueryJson.has("Vote")) {
                    if (userVoteQueryJson.getBoolean("Vote")) {
                        userVote = 1;
                    } else {
                        userVote = -1;
                    }
                } else {
                    userVote = 0;
                }
                JSONObject media = new JSONObject(item.getString("Media"));
                MediaPost mediaPost;
                if (media.has("text")) {
                    mediaPost = new TextPost(media.getJSONObject("text").getString("content"));
                } else if (media.has("image")) {
                    mediaPost = new ImagePost(media.getJSONObject("image").getString("url"));
                } else if (media.has("video")) {
                    mediaPost = new VideoPost(media.getJSONObject("video").getString("url"));
                } else {
                    throw new JSONException("Media is not of type text, image or video");
                }
                outputArray[i] = new FeedItem(i+1, mediaPost, item.getString("Submitter"), userVote, item.getInt("Votes"), item.getInt("PostID"));
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

    @RequestMapping("/getLatestFeedItems")
    public String getLatestFeedItems(@RequestBody GetFeedItemRequest request) {
        SQLClient sqlClient = new SQLClient();
        String sqlCommand = getSQLQuery(request.getLatitude(), request.getLongitude(), "Timestamp DESC", request.getGetPostsFrom(), request.getGetPostsTo());
        System.out.println(sqlCommand);
        JSONObject sqlOutput = sqlClient.getRows(sqlCommand);
        if (sqlOutput.has("error")) {
            sqlClient.terminate();
            return sqlOutput.toString();
        }
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getJwt());
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            FeedItem[] outputArray = new FeedItem[sqlArray.length()];
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
                int userVote;
                JSONObject userVoteQueryJson = sqlClient.getRow("SELECT * FROM users_votes WHERE (Email = '" + claims.getBody().getSubject() + "' AND PostID = '" + item.getString("PostID") + "')");
                if (userVoteQueryJson.has("Vote")) {
                    if (userVoteQueryJson.getBoolean("Vote")) {
                        userVote = 1;
                    } else {
                        userVote = -1;
                    }
                } else {
                    userVote = 0;
                }
                JSONObject media = new JSONObject(item.getString("Media"));
                MediaPost mediaPost;
                if (media.has("text")) {
                    mediaPost = new TextPost(media.getJSONObject("text").getString("content"));
                } else if (media.has("image")) {
                    mediaPost = new ImagePost(media.getJSONObject("image").getString("url"));
                } else if (media.has("video")) {
                    mediaPost = new VideoPost(media.getJSONObject("video").getString("url"));
                } else {
                    throw new JSONException("Media is not of type text, image or video");
                }
                outputArray[i] = new FeedItem(i+1, mediaPost, item.getString("Submitter"), userVote, item.getInt("Votes") - userVote, item.getInt("PostID"));
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

    public String getSQLQuery(final String latitude, final String longitude, final String orderBy, final String searchStart, final String searchEnd) {
        return "SELECT\n" +
                "    *, (\n" +
                "      3959 * acos (\n" +
                "      cos ( radians(" + latitude + ") )\n" +
                "      * cos( radians( Latitude ) )\n" +
                "      * cos( radians( Longitude ) - radians(" + longitude + ") )\n" +
                "      + sin ( radians(" + latitude + ") )\n" +
                "      * sin( radians( Latitude ) )\n" +
                "    )\n" +
                ") AS distance\n" +
                "FROM posts\n" +
                "HAVING distance < 5\n" +
                "ORDER BY " + orderBy + "\n" +
                "LIMIT " + searchStart + " , " + searchEnd + ";";
    }
}
