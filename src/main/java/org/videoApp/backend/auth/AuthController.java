package org.videoApp.backend.auth;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
import org.videoApp.backend.Profile;
import org.videoApp.backend.ProxilyJwtFilter;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.SavedLocations.SetLocationRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

@RestController
public class AuthController {

    Gson GSON = new Gson();
    private String DEFAULT_PROFILE_PICTURE = "";
    private String ENCRYPTION_KEY = System.getProperty("ProxilyEncryptionKey");
    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private SQLClient sqlClient;

    @RequestMapping("/auth/signin")
    public String signin(@RequestBody SignInRequest request) {
        PasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService();
        String sqlCommand = "SELECT * FROM users WHERE email=?";
        JSONArray values = new JSONArray();
        values.put(request.getEmail());
        JSONObject itemReturn = sqlClient.getRow(sqlCommand, values);
        try {
            String hashedPass = itemReturn.getString("HashedPassword");
            String salt = itemReturn.getString("Salt");
            JSONObject response = new JSONObject();
            if (passwordEncryptionService.authenticate(request.getPassword(), hashedPass, salt)) {
                String jws = Jwts.builder()
                        .setSubject(itemReturn.getString("UserID"))
                        .claim("firstName", itemReturn.getString("FirstName"))
                        .claim("lastName", itemReturn.getString("LastName"))
                        .claim("profilePicture", itemReturn.getString("ProfilePicture"))
                        .claim("email", request.getEmail())
                        .setIssuedAt(new Date())
                        .signWith(
                                SignatureAlgorithm.HS256,
                                ENCRYPTION_KEY.getBytes("UTF-8")
                        )
                        .compact();
                response.put("name", itemReturn.getString("FirstName") + " " + itemReturn.getString("LastName"));
                response.put("email", request.getEmail());
                response.put("profilePicture", itemReturn.getString("ProfilePicture"));
                response.put("jwt", "proxily." + jws);
            } else {
                response.put("jwt", "");
            }
            return response.toString();
        } catch (JSONException e) {
            LOG.error("JSONException when signing in: {}", e);
            return "{\"error\": \"internal server error\"}";
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOG.error("Exception when signing in: {}", e);
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException when signing in: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/service/checkToken")
    public String checkToken(@RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject response = new JSONObject();
            response.put("name", String.class.cast(claims.getBody().get("firstName")) + " " + String.class.cast(claims.getBody().get("lastName")));
            response.put("email", claims.getBody().get("email"));
            response.put("profilePicture", claims.getBody().get("profilePicture"));
            return response.toString();
        } catch (JSONException e) {
            LOG.error("JSONException when checking token: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/auth/signinFacebook")
    public String signinFacebook(@RequestBody String requestString) {
        try {
            JSONObject request = new JSONObject(requestString);
            JSONObject sqlPutJson = new JSONObject();
            ProxilyJwtFilter filter = new ProxilyJwtFilter();
            Profile profile = filter.getFacebookUserInfo(request.getString("token"));
            String sqlCommand = "SELECT * FROM oauths WHERE ServiceUserID=? AND Provider=?";
            JSONArray values = new JSONArray();
            values.put(profile.getId());
            values.put("facebook");
            JSONObject itemReturn = sqlClient.getRow(sqlCommand, values);
            String lastName = profile.getName().substring(profile.getName().lastIndexOf(' ') + 1).trim();
            String firstName = profile.getName().replace(" " + lastName, "");
            int userID;
            if (itemReturn.has("error") && itemReturn.get("error").equals("OBJECT_NOT_FOUND")) {
                sqlPutJson.put("Email", profile.getEmail());
                sqlPutJson.put("FirstName", firstName);
                sqlPutJson.put("LastName", lastName);
                sqlPutJson.put("ProfilePicture", profile.getPicture());
                userID = sqlClient.setRow(sqlPutJson, "users", false, true);
                sqlPutJson = new JSONObject();
                sqlPutJson.put("UserID", userID);
                sqlPutJson.put("ServiceUserID", profile.getId());
                sqlPutJson.put("Provider", "facebook");
                sqlClient.setRow(sqlPutJson, "oauths", false);
            } else {
                userID = itemReturn.getInt("UserID");
            }
            JSONObject response = new JSONObject();
            String jws = Jwts.builder()
                    .setSubject(Integer.toString(userID))
                    .claim("firstName", firstName)
                    .claim("lastName", lastName)
                    .claim("profilePicture", profile.getPicture())
                    .claim("email", profile.getEmail())
                    .setIssuedAt(new Date())
                    .signWith(
                            SignatureAlgorithm.HS256,
                            ENCRYPTION_KEY.getBytes("UTF-8")
                    )
                    .compact();
            response.put("name", profile.getName());
            response.put("email", profile.getEmail());
            response.put("profilePicture", profile.getPicture());
            response.put("jwt", "facebook." + jws);
            return response.toString();
        } catch (JSONException e) {
            LOG.error("JSONException when registering facebook account: {}", e);
            return "{error: \"Not a valid token.\"}";
        } catch (IOException e) {
            LOG.error("IOException when registering facebook account: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/auth/register")
    public String register(@RequestBody RegisterRequest request) {
        PasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService();
        try {
            JSONObject sqlPutJson = new JSONObject();
            String salt = passwordEncryptionService.generateSalt();
            String hashedPass = passwordEncryptionService.getEncryptedPassword(request.getPassword(), salt);
            sqlPutJson.put("Email", request.getEmail());
            sqlPutJson.put("FirstName", request.getFirstName());
            sqlPutJson.put("LastName", request.getLastName());
            sqlPutJson.put("ProfilePicture", DEFAULT_PROFILE_PICTURE);
            sqlPutJson.put("HashedPassword", hashedPass);
            sqlPutJson.put("Salt", salt);
            sqlClient.setRow(sqlPutJson, "users", false);
        } catch (JSONException e) {
            LOG.error("JSONException when registering account: {}", e);
            return "{error: \"internal server error\"}";
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOG.error("Exception when registering account: {}", e);
            return "{error: \"internal server error\"}";
        }
        JSONArray values = new JSONArray();
        values.put(request.getEmail());
        JSONObject user = sqlClient.getRow("SELECT * FROM users WHERE Email=?", values);
        try {
            String jws = Jwts.builder()
                    .setSubject(user.getString("UserID"))
                    .claim("firstName",request.getFirstName().substring(0, 1).toUpperCase() + request.getFirstName().substring(1))
                    .claim("lastName", request.getLastName().substring(0, 1).toUpperCase() + request.getLastName().substring(1))
                    .claim("email", request.getEmail())
                    .claim("profilePicture", DEFAULT_PROFILE_PICTURE)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis()+86400000L))
                    .signWith(
                            SignatureAlgorithm.HS256,
                            ENCRYPTION_KEY.getBytes("UTF-8")
                    )
                    .compact();
            JSONObject response = new JSONObject();
            response.put("token", "proxily." + jws);
            return response.toString();
        } catch (JSONException e) {
            LOG.error("JSONException when registering account: {}", e);
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException when registering account: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/service/updateSettings")
    public String updateSettings(@RequestBody String settings, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("UserID", claims.getBody().getSubject());
            sqlPutJson.put("SettingsField", settings);
            sqlClient.setRow(sqlPutJson, "user_settings", true, false);
            return "{\"success\": true}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }
}
