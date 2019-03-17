package org.videoApp.backend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class SQLClient {
    private Connection conn = null;

    public static Map<String, Class> TYPE;

    static {
        TYPE = new HashMap<String, Class>();

        TYPE.put("INTEGER", Integer.class);
        TYPE.put("INT", Integer.class);
        TYPE.put("TINYINT", Byte.class);
        TYPE.put("SMALLINT", Short.class);
        TYPE.put("BIGINT", Long.class);
        TYPE.put("REAL", Float.class);
        TYPE.put("FLOAT", Float.class);
        TYPE.put("DOUBLE", Double.class);
        TYPE.put("DECIMAL", BigDecimal.class);
        TYPE.put("NUMERIC", BigDecimal.class);
        TYPE.put("BOOLEAN", Boolean.class);
        TYPE.put("BOOL", Boolean.class);
        TYPE.put("BIT", Boolean.class);
        TYPE.put("CHAR", String.class);
        TYPE.put("VARCHAR", String.class);
        TYPE.put("LONGVARCHAR", String.class);
        TYPE.put("DATE", Date.class);
        TYPE.put("TIME", Time.class);
        TYPE.put("TIMESTAMP", Timestamp.class);
        TYPE.put("DATETIME", Timestamp.class);
        TYPE.put("SERIAL",Integer.class);
        // ...
    }

    public SQLClient() {
        try {
            // db parameters
            String url       = "jdbc:mysql://localhost:3306/Proxily?autoReconnect=true&useSSL=false";
            String user      = "root";
            String password  = "";

            // create a connection to the database
            conn = DriverManager.getConnection(url, user, password);

        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean executeCommand(String command) {
        try {
            Statement stmt = conn.createStatement();
            return stmt.execute(command);
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public JSONObject getRow(String command, JSONArray values) {
        try {
            if ((command.length() - command.replaceAll("\\?","").length()) != values.length()) {
                throw new Exception("The number of values does not match the statement: " + command);
            }
            PreparedStatement stmt = conn.prepareStatement(command);
            for (int i = 0; i < values.length(); i++) {
                stmt.setObject(i+1, values.get(i));
            }
            ResultSet rset = stmt.executeQuery();
            if (rset.next() == false) {
                JSONObject json = new JSONObject();
                json.put("error", "OBJECT_NOT_FOUND");
                return json;
            }
            JSONObject json = getEntityFromResultSet(rset);
            return json;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public JSONObject getRows(String command, JSONArray values) {
        try {
            if ((command.length() - command.replaceAll("\\?","").length()) != values.length()) {
                throw new Exception("The number of values does not match the statement: " + command);
            }
            PreparedStatement stmt = conn.prepareStatement(command);
            for (int i = 0; i < values.length(); i++) {
                stmt.setObject(i+1, values.get(i));
            }
            ResultSet rset = stmt.executeQuery();
            JSONObject json = getEntitiesFromResultSet(rset);
            return json;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void setRow(JSONObject command, String tableName, boolean override) {
        try {
            Statement stmt = conn.createStatement();
            StringBuilder cmdBuilder = new StringBuilder();
            if (override) {
                cmdBuilder.append("replace into " + tableName + " (");
            } else {
                cmdBuilder.append("insert into " + tableName + " (");
            }
            Iterator<String> itr = command.keys();
            for (int i = 0; i < command.length()-1; i++) {
                String key = itr.next();
                cmdBuilder.append(key + ", ");
            }
            String key = itr.next();
            cmdBuilder.append(key + ")");
            cmdBuilder.append(" values(");
            Iterator<String> itr1 = command.keys();
            for (int i = 0; i < command.length()-1; i++) {
                String key1 = itr1.next();
                Object object = command.get(key1);
                if (object instanceof String) {
                    cmdBuilder.append("\'" + command.getString(key1) + "\', ");
                    continue;
                }
                if (object instanceof Long) {
                    cmdBuilder.append("\'" + command.get(key1) + "\', ");
                    continue;
                }
                if (object instanceof Float) {
                    cmdBuilder.append("\'" + command.get(key1) + "\', ");
                    continue;
                }
                if (object instanceof Integer) {
                    cmdBuilder.append("\'" + command.getInt(key1) + "\', ");
                    continue;
                }
            }
            String key1 = itr1.next();
            Object object = command.get(key1);
            if (object instanceof String) {
                cmdBuilder.append("\"" + command.getString(key1) + "\")");
            }
            if (object instanceof Integer) {
                cmdBuilder.append(command.getInt(key1) + ")");
            }
            String cmd = cmdBuilder.toString();
            System.out.println(cmd);
            stmt.executeUpdate(cmd);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    protected JSONObject getEntitiesFromResultSet(ResultSet resultSet) throws SQLException, JSONException {
        if (resultSet.next() == false) {
            JSONObject json = new JSONObject();
            json.put("error", "OBJECT_NOT_FOUND");
            return json;
        }
        JSONArray entities = new JSONArray();
        entities.put(getEntityFromResultSet(resultSet));
        while (resultSet.next()) {
            entities.put(getEntityFromResultSet(resultSet));
        }
        JSONObject json = new JSONObject();
        json.put("entries", entities);
        return json;
    }

    protected JSONObject getEntityFromResultSet(ResultSet resultSet) throws SQLException, JSONException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        JSONObject json = new JSONObject();
        for (int i = 1; i <= columnCount; ++i) {
            Class castType = SQLClient.TYPE.get(metaData.getColumnTypeName(i).toUpperCase());
            json.put(metaData.getColumnName(i), resultSet.getObject(i, castType));
        }
        return json;
    }



    public void terminate() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch(SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}