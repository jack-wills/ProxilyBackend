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
import org.videoApp.backend.SQLClient;

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
                response.put("firstName", itemReturn.getString("FirstName"));
                response.put("lastName", itemReturn.getString("LastName"));
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
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(request.getString("jwt"));
            claims.getBody().getSubject();
            JSONObject response = new JSONObject();
            response.put("firstName", claims.getBody().get("firstName"));
            response.put("lastName", claims.getBody().get("lastName"));
            response.put("email", claims.getBody().getSubject());
            return response.toString();
        } catch (JSONException e) {
            return "{\"error\": \"internal server error\"}";
        } catch (UnsupportedEncodingException e) {
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
