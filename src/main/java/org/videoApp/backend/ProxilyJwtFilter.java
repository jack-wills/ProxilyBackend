package org.videoApp.backend;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultJws;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ProxilyJwtFilter extends GenericFilterBean {

    private static final Logger LOG = LoggerFactory.getLogger(ProxilyJwtFilter.class);
    private static String DECRYPTION_KEY = System.getProperty("ProxilyEncryptionKey");

    @Autowired
    private SQLClient sqlClient;

    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        final String authHeader = request.getHeader("authorization");

        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);

            chain.doFilter(req, res);
        } else {

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new ServletException("Missing or invalid Authorization header");
            }

            final String token = authHeader.substring(7);

            try {
                request.setAttribute("claims", decodeToken(token));
            } catch (UnauthorisedException e) {
                throw new ServletException("Missing or invalid Authorization header");
            } catch (JSONException e) {

            }
        }

        chain.doFilter(req, res);
    }

    public Profile getFacebookUserInfo(String accessToken) throws IOException, JSONException {
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

    public Jws<Claims> decodeToken(String accessToken) throws IOException, UnauthorisedException, JSONException {
        LOG.info("token: " + accessToken);
        String provider = accessToken.subSequence(0, accessToken.indexOf(".")).toString();
        accessToken = accessToken.replace(provider + ".", "");
        if (provider.equals("proxily")) {
            return Jwts.parser()
                    .setSigningKey(DECRYPTION_KEY.getBytes("UTF-8"))
                    .parseClaimsJws(accessToken);
        } else if (provider.equals("facebook")) {
            Profile profile;
            try {
                profile = getFacebookUserInfo(accessToken);
            } catch (JSONException f) {
                LOG.info("User not authenicated! Token = " + accessToken);
                throw new UnauthorisedException();
            }

            Map<String, Object> map = new HashMap<>();
            JSONArray values = new JSONArray();
            values.put(profile.getId());
            values.put(provider);
            String sqlCommand = "SELECT * FROM oauths WHERE ServiceUserID=? AND Provider=?";
            JSONObject sqlJson = sqlClient.getRow(sqlCommand, values);
            String userID = Integer.toString(sqlJson.getInt("UserID"));
            map.put("sub", userID);
            map.put("email", profile.getEmail());
            String lastName = profile.getName().substring(profile.getName().lastIndexOf(' ') + 1).trim();
            String firstName = profile.getName().replace(" " + lastName, "");
            map.put("firstName", firstName);
            map.put("lastName", lastName);
            return new DefaultJws<>(null, new DefaultClaims(map), null);
        } else {
            LOG.warn("Provider not valid: " + provider);
            throw new UnauthorisedException();
        }
    }
}
