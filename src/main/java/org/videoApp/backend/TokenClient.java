package org.videoApp.backend;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultJws;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TokenClient {
    private static final Logger LOG = LoggerFactory.getLogger(TokenClient.class);

    public static Profile getFacebookUserInfo(String accessToken) throws IOException, JSONException {
        String url = "https://graph.facebook.com/v2.12/me?fields=id,name,picture,email&access_token=" + accessToken;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("GET");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONObject myResponse = new JSONObject(response.toString());
        JSONObject picture_reponse = myResponse.getJSONObject("picture");
        JSONObject data_response = picture_reponse.getJSONObject("data");
        return new Profile(myResponse.getString("id"), myResponse.getString("email"), myResponse.getString("name"), data_response.getString("url"));
    }

    public static Jws<Claims> decodeToken(String accessToken) throws IOException, UnauthorisedException {
        try {
            return Jwts.parser()
                    .setSigningKey("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=".getBytes("UTF-8"))
                    .parseClaimsJws(accessToken);
        } catch (MalformedJwtException e) {
            Profile profile;
            try {
                profile = getFacebookUserInfo(accessToken);
            } catch (JSONException f) {
                LOG.info("User not authenicated! Token = " + accessToken);
                throw new UnauthorisedException();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("sub", profile.getId());
            map.put("email", profile.getEmail());
            String lastName = profile.getName().substring(profile.getName().lastIndexOf(' ') + 1).trim();
            String firstName = profile.getName().replace(" " + lastName, "");
            map.put("firstName", firstName);
            map.put("lastName", lastName);
            return new DefaultJws<>(null, new DefaultClaims(map), null);
        }
    }
}
