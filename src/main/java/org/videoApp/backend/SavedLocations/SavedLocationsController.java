package org.videoApp.backend.SavedLocations;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.videoApp.backend.SQLClient;
import org.videoApp.backend.TokenClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@RestController
public class SavedLocationsController {

    Gson GSON = new Gson();
    @RequestMapping("/getSavedLocations")
    public String getSavedLocations(@RequestBody String requestString) {
        SQLClient sqlClient = new SQLClient();
        String sqlCommand = "SELECT SavedLocationID, Name, Latitude, Longitude FROM saved_locations WHERE UserID=?;";
        try {
            JSONObject request = new JSONObject(requestString);
            Jws<Claims> claims = TokenClient.decodeToken(request.getString("jwt"));
            JSONArray values = new JSONArray();
            values.put(claims.getBody().getSubject());
            JSONObject sqlOutput = sqlClient.getRows(sqlCommand, values);

            if (sqlOutput.has("error")) {
                sqlClient.terminate();
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            sqlClient.terminate();
            return sqlArray
                    .toString()
                    .replace("\"SavedLocationID\"", "\"id\"")
                    .replace("\"Latitude\"", "\"latitude\"")
                    .replace("\"Longitude\"", "\"longitude\"")
                    .replace("\"Name\"", "\"name\"");
        } catch (JSONException e) {
            sqlClient.terminate();
            System.out.println("JSONException: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (UnsupportedEncodingException e) {
            sqlClient.terminate();
            System.out.println("UnsupportedEncodingException: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (IOException e) {
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/putSavedLocation")
    public String putSavedLocation(@RequestBody PutSavedLocationRequest request) {
        SQLClient sqlClient = new SQLClient();
        try {
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("UserID", claims.getBody().getSubject());
            sqlPutJson.put("Name", request.getName());
            sqlPutJson.put("Latitude", request.getLatitude());
            sqlPutJson.put("Longitude", request.getLongitude());
            int savedLocationID = sqlClient.setRow(sqlPutJson, "saved_locations", false, true);
            sqlClient.terminate();
            if (savedLocationID == 0) {
                return "{\"error\": \"internal server error\"}";
            }
            JSONObject response = new JSONObject();
            response.put("id", savedLocationID);
            return response.toString();
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

    @RequestMapping("/removeSavedLocation")
    public String removeSavedLocation(@RequestBody RemoveSavedLocationRequest request) {
        SQLClient sqlClient = new SQLClient();
        String sqlCommand = "DELETE FROM saved_locations WHERE SavedLocationID=? AND UserID=?";
        try {
            Jws<Claims> claims = TokenClient.decodeToken(request.getJwt());
            JSONArray values = new JSONArray();
            values.put(request.getID());
            values.put(claims.getBody().getSubject());
            sqlClient.deleteRows(sqlCommand, values);
            sqlClient.terminate();
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
}
