package org.videoApp.backend.auth;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.videoApp.backend.Profile;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.TokenClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

@RestController
public class AuthController {

    Gson GSON = new Gson();

    @RequestMapping("/signin")
    public String signin(@RequestBody SignInRequest request) {
        SQLClient sqlClient = new SQLClient();
        PasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService();
        JSONObject itemReturn = sqlClient.getRow("SELECT * FROM users WHERE email='" + request.getEmail() + "'");
        try {
            String hashedPass = itemReturn.getString("HashedPassword");
            String salt = itemReturn.getString("Salt");
            JSONObject response = new JSONObject();
            if (passwordEncryptionService.authenticate(request.getPassword(), hashedPass, salt)) {
                String jws = Jwts.builder()
                        .setSubject(request.getEmail())
                        .claim("firstName", itemReturn.getString("FirstName"))
                        .claim("lastName", itemReturn.getString("LastName"))
                        .setIssuedAt(new Date())
                        .signWith(
                                SignatureAlgorithm.HS256,
                                "Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8")
                        )
                        .compact();
                response.put("name", itemReturn.getString("FirstName") + " " + itemReturn.getString("LastName"));
                response.put("email", request.getEmail());
                response.put("jwt", jws);
            } else {
                response.put("jwt", "");
            }
            sqlClient.terminate();
            return response.toString();
        } catch (JSONException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/checkToken")
    public String checkToken(@RequestBody String requestString) {
        try {
            JSONObject request = new JSONObject(requestString);
            Jws<Claims> claims = TokenClient.decodeToken(request.getString("token"));
            claims.getBody().getSubject();
            JSONObject response = new JSONObject();
            response.put("name", String.class.cast(claims.getBody().get("firstName")) + " " + String.class.cast(claims.getBody().get("lastName")));
            response.put("email", claims.getBody().getSubject());
            return response.toString();
        } catch (JSONException e) {
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            return "{\"error\": \"internal server error\"}";
        } catch (IOException e) {
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/registerFacebookAccount")
    public String registerFacebookAccount(@RequestBody String requestString) {
        SQLClient sqlClient = new SQLClient();
        try {
            JSONObject request = new JSONObject(requestString);
            JSONObject sqlPutJson = new JSONObject();
            Profile profile = TokenClient.getFacebookUserInfo(request.getString("token"));
            JSONObject itemReturn = sqlClient.getRow("SELECT * FROM users WHERE email='" + profile.getEmail() + "'");
            if (itemReturn.has("error") && itemReturn.get("error").equals("OBJECT_NOT_FOUND")) {
                sqlPutJson.put("Email", profile.getEmail());
                String lastName = profile.getName().substring(profile.getName().lastIndexOf(' ') + 1).trim();
                String firstName = profile.getName().replace(" " + lastName, "");
                sqlPutJson.put("FirstName", firstName);
                sqlPutJson.put("LastName", lastName);
                sqlClient.setRow(sqlPutJson, "users", false);
            }
            sqlClient.terminate();
            return "{\"success\": true}";
        } catch (JSONException e) {
            sqlClient.terminate();
            return "{error: \"internal server error\"}";
        } catch (IOException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        SQLClient sqlClient = new SQLClient();
        PasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService();
        try {
            JSONObject sqlPutJson = new JSONObject();
            String salt = passwordEncryptionService.generateSalt();
            String hashedPass = passwordEncryptionService.getEncryptedPassword(request.getPassword(), salt);
            sqlPutJson.put("Email", request.getEmail());
            sqlPutJson.put("FirstName", request.getFirstName());
            sqlPutJson.put("LastName", request.getLastName());
            sqlPutJson.put("HashedPassword", hashedPass);
            sqlPutJson.put("Salt", salt);
            sqlClient.setRow(sqlPutJson, "users", false);
        } catch (JSONException e) {
            sqlClient.terminate();
            return "{error: \"internal server error\"}";
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            sqlClient.terminate();
            return "{error: \"internal server error\"}";
        }

        try {
            String jws = Jwts.builder()
                    .setSubject(request.getEmail())
                    .claim("firstName",request.getFirstName())
                    .claim("lastName", request.getLastName())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis()+86400000L))
                    .signWith(
                            SignatureAlgorithm.HS256,
                            "Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8")
                    )
                    .compact();
            JSONObject response = new JSONObject();
            response.put("token", jws);
            sqlClient.terminate();
            return response.toString();
        } catch (JSONException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            return "{\"error\": \"internal server error\"}";
        }
    }
}