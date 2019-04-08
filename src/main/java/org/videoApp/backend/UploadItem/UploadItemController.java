package org.videoApp.backend.UploadItem;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.UnauthorisedException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

@RestController
public class UploadItemController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadItemController.class);

    @Autowired
    private SQLClient sqlClient;

    @RequestMapping("/service/uploadItem")
    public String uploadItem(@RequestBody UploadItemRequest request, @RequestAttribute Jws<Claims> claims) {
        LocalDateTime ldt = LocalDateTime.now(Clock.systemUTC());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            String responseUrl = "";
            JSONObject media = new JSONObject();
            JSONObject mediaContent = new JSONObject();
            if (request.getMediaType().equals("text")) {
                mediaContent.put("content", request.getMedia());
                media.put(request.getMediaType(), mediaContent);
            } else if (request.getMediaType().equals("image")) {
                AmazonS3 client = getS3Client();
                GeneratePresignedUrlRequest s3RequestUpload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-image-us-east-1",
                                claims.getBody().getSubject() + "_" + format.format(ldt) + ".jpeg")
                                .withExpiration(Date.from(Instant.now().plusSeconds(3600)))
                                .withMethod(HttpMethod.PUT);
                responseUrl = client.generatePresignedUrl(s3RequestUpload).toString();

                GeneratePresignedUrlRequest s3RequestDownload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-image-us-east-1",
                                claims.getBody().getSubject() + "_" + format.format(ldt) + ".jpeg")
                                .withExpiration(Date.from(Instant.now().plusSeconds(604800))) //7 days
                                .withMethod(HttpMethod.GET);
                mediaContent.put("url", client.generatePresignedUrl(s3RequestDownload).toString());
                media.put(request.getMediaType(), mediaContent);
            } else if (request.getMediaType().equals("video")) {
                AmazonS3 client = getS3Client();
                GeneratePresignedUrlRequest s3RequestUpload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-video-us-east-1",
                                claims.getBody().getSubject() + "_" + format.format(ldt) + ".mp4")
                                .withExpiration(Date.from(Instant.now().plusSeconds(3600)))
                                .withMethod(HttpMethod.PUT);
                responseUrl = client.generatePresignedUrl(s3RequestUpload).toString();

                GeneratePresignedUrlRequest s3RequestDownload =
                        new GeneratePresignedUrlRequest(
                                "proxily-post-video-us-east-1",
                                claims.getBody().getSubject() + "_" + format.format(ldt) + ".mp4")
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
            sqlPutJson.put("Timestamp", format.format(ldt));
            sqlPutJson.put("FileUploaded", request.getMediaType().equals("text") ? 1 : 0);
            sqlClient.setRow(sqlPutJson, "posts", false);
            if (responseUrl != "") {
                JSONObject response = new JSONObject();
                response.put("uploadUrl", responseUrl);
                return response.toString();
            }
            return "{\"success\": true}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
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
