package org.videoApp.backend.SavedLocations;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.videoApp.backend.SQLClient;

@RestController
public class SavedLocationsController {

    private static final Logger LOG = LoggerFactory.getLogger(SavedLocationsController.class);

    @Autowired
    private SQLClient sqlClient;

    @RequestMapping("/service/getSavedLocations")
    public String getSavedLocations(@RequestAttribute Jws<Claims> claims) {
        String sqlCommand = "SELECT SavedLocationID, Name, Latitude, Longitude FROM saved_locations WHERE UserID=?;";
        try {
            JSONArray values = new JSONArray();
            values.put(claims.getBody().getSubject());
            JSONObject sqlOutput = sqlClient.getRows(sqlCommand, values);

            if (sqlOutput.has("error")) {
                if (sqlOutput.getString("error").equals("OBJECT_NOT_FOUND")) {
                    return "[]";
                }
                return sqlOutput.toString();
            }
            JSONArray sqlArray = sqlOutput.getJSONArray("entries");
            return sqlArray
                    .toString()
                    .replace("\"SavedLocationID\"", "\"id\"")
                    .replace("\"Latitude\"", "\"latitude\"")
                    .replace("\"Longitude\"", "\"longitude\"")
                    .replace("\"Name\"", "\"name\"");
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping("/service/putSavedLocation")
    public String putSavedLocation(@RequestBody PutSavedLocationRequest request, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("UserID", claims.getBody().getSubject());
            sqlPutJson.put("Name", request.getName());
            sqlPutJson.put("Latitude", request.getLatitude());
            sqlPutJson.put("Longitude", request.getLongitude());
            int savedLocationID = sqlClient.setRow(sqlPutJson, "saved_locations", false, true);
            if (savedLocationID == 0) {
                return "{\"error\": \"internal server error\"}";
            }
            JSONObject response = new JSONObject();
            response.put("id", savedLocationID);
            return response.toString();
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }

    @RequestMapping("/service/removeSavedLocation")
    public String removeSavedLocation(@RequestBody RemoveSavedLocationRequest request, @RequestAttribute Jws<Claims> claims) {
        String sqlCommand = "DELETE FROM saved_locations WHERE SavedLocationID=? AND UserID=?";
        JSONArray values = new JSONArray();
        values.put(request.getID());
        values.put(claims.getBody().getSubject());
        sqlClient.deleteRows(sqlCommand, values);
        return "{\"success\": true}";
    }

    @RequestMapping("/service/setLocation")
    public String setLocation(@RequestBody SetLocationRequest request, @RequestAttribute Jws<Claims> claims) {
        try {
            JSONObject sqlPutJson = new JSONObject();
            sqlPutJson.put("UserID", claims.getBody().getSubject());
            sqlPutJson.put("Latitude", request.getLatitude());
            sqlPutJson.put("Longitude", request.getLongitude());
            sqlClient.setRow(sqlPutJson, "user_location", true, false);
            return "{\"success\": true}";
        } catch (JSONException e) {
            LOG.error("JSONException: {}", e);
            return "{\"error\": \"internal server error\"}";
        }
    }
}
