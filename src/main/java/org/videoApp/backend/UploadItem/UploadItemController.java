package org.videoApp.backend.UploadItem;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.videoApp.backend.SQLClient;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
public class UploadItemController {

    Gson GSON = new Gson();

    @RequestMapping("/uploadItem")
    public String uploadItem(@RequestBody UploadItemRequest request) {
        SQLClient sqlClient = new SQLClient();
        LocalDateTime ldt = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            JSONObject media = new JSONObject();
            JSONObject mediaContent = new JSONObject();
            if (request.getMediaType().equals("text")) {
                mediaContent.put("content", request.getMedia());
                media.put(request.getMediaType(), mediaContent);
            } else if (request.getMediaType().equals("image")) {
                mediaContent.put("url", "file:///Users/Jack/Desktop/videoApp/assets/mountains.jpg");
                media.put(request.getMediaType(), mediaContent);
            } else if (request.getMediaType().equals("video")) {
                mediaContent.put("url", "file:///Users/Jack/Desktop/videoApp/assets/sample.mp4");
                media.put(request.getMediaType(), mediaContent);
            } else {
                return "{\"error\": \"internal server error\"}";
            }
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getJwt());
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("Media", media.toString());
            sqlPutJson.put("Submitter", claims.getBody().get("firstName") + " " + claims.getBody().get("lastName"));
            sqlPutJson.put("Votes", 0);
            sqlPutJson.put("Latitude", Float.valueOf(request.getLatitude()));
            sqlPutJson.put("Longitude", Float.valueOf(request.getLongitude()));
            sqlPutJson.put("Timestamp", formmat1.format(ldt));
            sqlPutJson.put("FileUploaded", request.getMediaType().equals("text") ? 1 : 0);
            sqlClient.setRow(sqlPutJson, "posts", false);
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
}
