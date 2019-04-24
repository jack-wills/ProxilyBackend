package org.videoApp.backend.GetFeedItem;
import com.google.gson.Gson;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.videoApp.backend.ProxilyJwtFilter;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.UnauthorisedException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@RestController
public class GetFeedController {

    private static final Logger LOG = LoggerFactory.getLogger(GetFeedController.class);

    @Autowired
    private SQLClient sqlClient;

    Gson GSON = new Gson();

    @RequestMapping("/service/getPopularFeedItems")
    public String getPopularFeedItems(@RequestBody GetFeedItemRequest request, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject sqlOutput = getSQLQuery(sqlClient, request.getLatitude(), request.getLongitude(), claims.getBody().getSubject(), "Votes*0.7 + (1/(NOW() - Timestamp))*0.3 DESC", request.getGetPostsFrom(), request.getGetPostsTo());
            if (sqlOutput.has("error")) {
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            FeedItem[] outputArray = new FeedItem[sqlArray.length()];
            JSONObject markPostSeen = new JSONObject();
            JSONArray markPostSeenUserID = new JSONArray();
            JSONArray markPostSeenPostID = new JSONArray();
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
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
                int userVote;
                if (item.has("Vote")) {
                    userVote = item.getBoolean("Vote") ? 1 : -1;
                } else {
                    userVote = 0;
                }
                markPostSeenUserID.put(claims.getBody().getSubject());
                markPostSeenPostID.put(item.getInt("PostID"));
                outputArray[i] = new FeedItem(i + 1,
                        mediaPost,
                        item.getString("FirstName") + " " + item.getString("LastName"),
                        item.getString("ProfilePicture"),
                        userVote,
                        item.getInt("Votes") - userVote,
                        item.getInt("PostID"),
                        claims.getBody().getSubject().equals(item.getString("UserID")));
            }
            markPostSeen.put("UserID", markPostSeenUserID);
            markPostSeen.put("PostID", markPostSeenPostID);
            sqlClient.setRows(markPostSeen, "viewed_posts", sqlArray.length(),true);
            return GSON.toJson(outputArray);
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping("/service/getLatestFeedItems")
    public String getLatestFeedItems(@RequestBody GetFeedItemRequest request, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject sqlOutput = getSQLQuery(sqlClient, request.getLatitude(), request.getLongitude(), claims.getBody().getSubject(), "Timestamp DESC", request.getGetPostsFrom(), request.getGetPostsTo());
            LOG.info(sqlOutput.toString());
            if (sqlOutput.has("error")) {
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            FeedItem[] outputArray = new FeedItem[sqlArray.length()];
            JSONObject markPostSeen = new JSONObject();
            JSONArray markPostSeenUserID = new JSONArray();
            JSONArray markPostSeenPostID = new JSONArray();
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
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
                int userVote;
                if (item.has("Vote")) {
                    userVote = item.getBoolean("Vote") ? 1 : -1;
                } else {
                    userVote = 0;
                }
                markPostSeenUserID.put(claims.getBody().getSubject());
                markPostSeenPostID.put(item.getInt("PostID"));
                outputArray[i] = new FeedItem(i+1,
                        mediaPost,
                        item.getString("FirstName") + " " + item.getString("LastName"),
                        item.getString("ProfilePicture"),
                        userVote,
                        item.getInt("Votes") - userVote,
                        item.getInt("PostID"),
                        claims.getBody().getSubject().equals(item.getString("UserID")));
            }
            markPostSeen.put("UserID", markPostSeenUserID);
            markPostSeen.put("PostID", markPostSeenPostID);
            sqlClient.setRows(markPostSeen, "viewed_posts", sqlArray.length(),true);
            return GSON.toJson(outputArray);
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public JSONObject getSQLQuery(
            final SQLClient sqlClient,
            final float latitude,
            final float longitude,
            final String userId,
            final String orderBy,
            final int searchStart,
            final int searchEnd) throws JSONException {
        String sqlCommand =  "SELECT\n" +
                "    posts.*, users.FirstName, users.LastName, users.ProfilePicture, reported_posts.ReportID, users_votes.Vote, (\n" +
                "      3959 * acos (\n" +
                "      cos ( radians(?) )\n" +
                "      * cos( radians( Latitude ) )\n" +
                "      * cos( radians( Longitude ) - radians(?) )\n" +
                "      + sin ( radians(?) )\n" +
                "      * sin( radians( Latitude ) )\n" +
                "    )\n" +
                ") AS distance\n" +
                "FROM posts\n" +
                "INNER JOIN users ON posts.UserID = users.UserID\n" +
                "LEFT OUTER JOIN reported_posts ON ? = reported_posts.UserID AND posts.PostID = reported_posts.PostID\n" +
                "LEFT OUTER JOIN users_votes ON ? = users_votes.UserID AND posts.PostID = users_votes.PostID\n" +
                "WHERE FileUploaded = 1 AND ReportID is NULL\n" +
                "HAVING distance < 15\n" +
                "ORDER BY " + orderBy + "\n" +
                "LIMIT ? , ?;";
        JSONArray values = new JSONArray();
        values.put(latitude);
        values.put(longitude);
        values.put(latitude);
        values.put(userId);
        values.put(userId);
        values.put(searchStart);
        values.put(searchEnd);
        return sqlClient.getRows(sqlCommand, values);
    }

    @RequestMapping("/getPost")
    public String getPost(@RequestBody String requestString) {
        try {
            JSONObject request = new JSONObject(requestString);
            String sqlCommand =  "SELECT posts.*, users.FirstName, users.LastName, users.ProfilePicture FROM posts INNER JOIN users ON posts.UserID = users.UserID WHERE PostID=?";
            JSONArray values = new JSONArray();
            values.put(request.getString("postID"));
            JSONObject item = sqlClient.getRow(sqlCommand, values);
            if (item.has("error")) {
                return item.toString();
            }
            int userVote;
            String userID = "";
            if (request.has("token")) {
                ProxilyJwtFilter filter = new ProxilyJwtFilter();
                Jws<Claims> claims = filter.decodeToken(request.getString("token"));
                userID = claims.getBody().getSubject();
                values = new JSONArray();
                values.put(claims.getBody().getSubject());
                values.put(item.getString("PostID"));
                JSONObject userVoteQueryJson = sqlClient.getRow("SELECT * FROM users_votes WHERE (UserID = ? AND PostID = ?)", values);
                if (userVoteQueryJson.has("Vote")) {
                    if (userVoteQueryJson.getBoolean("Vote")) {
                        userVote = 1;
                    } else {
                        userVote = -1;
                    }
                } else {
                    userVote = 0;
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
            return GSON.toJson(new FeedItem(0,
                    mediaPost,
                    item.getString("FirstName") + " " + item.getString("LastName"),
                    item.getString("ProfilePicture"),
                    userVote,
                    item.getInt("Votes") - userVote,
                    item.getInt("PostID"),
                    userID.equals(item.getString("UserID"))));
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (IOException e) {
            LOG.error("IOException: {}", e);
            return "{\"error\": \"internal server error\"}";
        } catch (UnauthorisedException e) {
            return "{\"error\": \"Not a valid token.\"}";
        }
    }

    @RequestMapping("/service/getMyPosts")
    public String getMyPosts(@RequestAttribute Jws<Claims> claims) {
        try {
            String sqlCommand = "SELECT posts.*, users_votes.Vote FROM posts LEFT OUTER JOIN users_votes ON ? = users_votes.UserID AND posts.PostID = users_votes.PostID WHERE posts.UserID=? AND FileUploaded=1 ORDER BY (Votes*0.7 + (1/(NOW() - Timestamp))*0.3) DESC";
            JSONArray values = new JSONArray();
            values.put(claims.getBody().getSubject());
            values.put(claims.getBody().getSubject());
            JSONObject sqlOutput = sqlClient.getRows(sqlCommand, values);
            if (sqlOutput.has("error")) {
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            JSONArray outputArray = new JSONArray();
            for (int i = 0; i < sqlArray.length(); i++) {
                JSONObject item = sqlArray.getJSONObject(i);
                int userVote;
                if (item.has("Vote")) {
                    userVote = item.getBoolean("Vote") ? 1 : -1;
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
                JSONObject itemReturn = new JSONObject();
                itemReturn.put("id", i);
                itemReturn.put("submitter", claims.getBody().get("firstName") + " " + claims.getBody().get("lastName"));
                itemReturn.put("submitterProfilePicture", claims.getBody().get("profilePicture"));
                itemReturn.put("userVote", userVote);
                itemReturn.put("media", new JSONObject(GSON.toJson(mediaPost)));
                itemReturn.put("totalVotes", item.getInt("Votes") - userVote);
                itemReturn.put("postId", item.getInt("PostID"));
                itemReturn.put("requestersPost", true);
                outputArray.put(itemReturn);
            }
            return outputArray.toString();
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
