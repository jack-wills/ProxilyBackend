package org.videoApp.backend.UploadItem;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
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
import org.videoApp.backend.TokenClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

@RestController
public class UploadItemController {

    Gson GSON = new Gson();

    @RequestMapping("/uploadItem")
    public String uploadItem(@RequestBody UploadItemRequest request) {
        SQLClient sqlClient = new SQLClient();
        LocalDateTime ldt = LocalDateTime.now(Clock.systemUTC());
        DateTimeFormatter sqlFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        DateTimeFormatter s3Format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH);
        try {
            String responseUrl = "";
            JSONObject media = new JSONObject();
            JSONObject mediaContent = new JSONObject();
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
            if (request.getMediaType().equals("text")) {
                mediaContent.put("content", request.getMedia());
                media.put(request.getMediaType(), mediaContent);
            } else if (request.getMediaType().equals("image")) {
                AmazonS3 client = getS3Client();
                GeneratePresignedUrlRequest s3RequestUpload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-images-us-east-1",
                                claims.getBody().getSubject() + "_" + s3Format.format(ldt) + ".jpeg")
                                .withExpiration(Date.from(Instant.now().plusSeconds(3600)))
                                .withMethod(HttpMethod.PUT);
                responseUrl = client.generatePresignedUrl(s3RequestUpload).toString();

                GeneratePresignedUrlRequest s3RequestDownload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-images-us-east-1",
                                claims.getBody().getSubject() + "_" + s3Format.format(ldt) + ".jpeg")
                                .withExpiration(Date.from(Instant.now().plusSeconds(604800))) //7 days
                                .withMethod(HttpMethod.GET);
                mediaContent.put("url", client.generatePresignedUrl(s3RequestDownload).toString());
                media.put(request.getMediaType(), mediaContent);
            } else if (request.getMediaType().equals("video")) {
                AmazonS3 client = getS3Client();
                GeneratePresignedUrlRequest s3RequestUpload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-videos-us-east-1",
                                claims.getBody().getSubject() + "_" + s3Format.format(ldt) + ".mp4")
                                .withExpiration(Date.from(Instant.now().plusSeconds(3600)))
                                .withMethod(HttpMethod.PUT);
                responseUrl = client.generatePresignedUrl(s3RequestUpload).toString();

                GeneratePresignedUrlRequest s3RequestDownload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-videos-us-east-1",
                                claims.getBody().getSubject() + "_" + s3Format.format(ldt) + ".mp4")
                                .withExpiration(Date.from(Instant.now().plusSeconds(604800))) //7 days
                                .withMethod(HttpMethod.GET);
                mediaContent.put("url", client.generatePresignedUrl(s3RequestDownload).toString());
                media.put(request.getMediaType(), mediaContent);
            } else {
                return "{\"error\": \"internal server error\"}";
            }
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("Media", media.toString());
            sqlPutJson.put("UserID", claims.getBody().getSubject());
            sqlPutJson.put("Votes", 0);
            sqlPutJson.put("Latitude", Float.valueOf(request.getLatitude()));
            sqlPutJson.put("Longitude", Float.valueOf(request.getLongitude()));
            sqlPutJson.put("Timestamp", sqlFormat.format(ldt));
            sqlPutJson.put("FileUploaded", request.getMediaType().equals("text") ? 1 : 0);
            sqlClient.setRow(sqlPutJson, "posts", false);
            sqlClient.terminate();
            if (responseUrl != "") {
                JSONObject response = new JSONObject();
                response.put("uploadUrl", responseUrl);
                return response.toString();
            }
            return "{\"success\": true}";
        } catch (JSONException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        } catch (IOException e) {
            return "{\"error\": \"internal server error\"}";
        }
    }

    private AmazonS3 getS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(Regions.US_EAST_1)
                .build();
    }
}
