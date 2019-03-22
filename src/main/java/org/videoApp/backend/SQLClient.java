package org.videoApp.backend;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class SQLClient {
    private Connection conn;
    private Region region = Regions.getCurrentRegion();

    private static String SSL_CERTIFICATE;

    private static final String KEY_STORE_TYPE = "JKS";
    private static final String KEY_STORE_PROVIDER = "SUN";
    private static final String KEY_STORE_FILE_PREFIX = "sys-connect-via-ssl-test-cacerts";
    private static final String KEY_STORE_FILE_SUFFIX = ".jks";
    private static final String DEFAULT_KEY_STORE_PASSWORD = "changeit";

    public SQLClient() {
        try {
            // create a connection to the database
            if (region == null) {
                String url       = "jdbc:mysql://localhost:3306/Proxily?autoReconnect=true&useSSL=false";
                String user      = "root";
                String password  = "";
                conn = DriverManager.getConnection(url, user, password);
            } else {
                SSL_CERTIFICATE = "rds-ca-2015-" +  Regions.getCurrentRegion().toString() + ".pem";
                AmazonRDS rdsClient = AmazonRDSClientBuilder
                        .standard()
                        .withCredentials(new DefaultAWSCredentialsProviderChain())
                        .withRegion(region.getName())
                        .build();

                DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
                DescribeDBInstancesResult result = rdsClient.describeDBInstances(request);
                List<DBInstance> list = result.getDBInstances();
                conn = getDBConnectionUsingIam(list.get(0).getEndpoint().getAddress(),
                        list.get(0).getEndpoint().getPort(),
                        "admin");
            }

        } catch(Exception e) {
            System.out.println(e.getMessage());
            throw new IllegalStateException(e.getMessage());
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

    public int deleteRows(String command, JSONArray values) {
        try {
            if ((command.length() - command.replaceAll("\\?","").length()) != values.length()) {
                throw new Exception("The number of values does not match the statement: " + command);
            }
            PreparedStatement stmt = conn.prepareStatement(command);
            for (int i = 0; i < values.length(); i++) {
                stmt.setObject(i+1, values.get(i));
            }
            return stmt.executeUpdate();
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
        setRow(command, tableName, override, false);
    }

    public int setRow(JSONObject command, String tableName, boolean override, boolean returnID) {
        try {
            StringBuilder cmdBuilder = new StringBuilder();
            if (override) {
                cmdBuilder.append("replace into " + tableName + " (");
            } else {
                cmdBuilder.append("insert into " + tableName + " (");
            }
            Iterator<String> itr = command.keys();
            for (int i = 0; i < command.length()-1; i++) {
                cmdBuilder.append(itr.next() + ", ");
            }
            cmdBuilder.append(itr.next() + ") values(");
            for (int i = 0; i < command.length()-1; i++) {
                cmdBuilder.append("?, ");
            }
            PreparedStatement stmt;
            cmdBuilder.append("?);");
            if (returnID) {
                stmt = conn.prepareStatement(cmdBuilder.toString(), Statement.RETURN_GENERATED_KEYS);
            } else {
                stmt = conn.prepareStatement(cmdBuilder.toString());
            }

            Iterator<String> itr1 = command.keys();
            for (int i = 0; i < command.length(); i++) {
                String key1 = itr1.next();
                stmt.setObject(i+1, command.get(key1));
            }
            stmt.executeUpdate();
            if (!returnID) {
                return 0;
            }
            ResultSet rset = stmt.getGeneratedKeys();
            rset.next();
            return rset.getInt(1);
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
            json.put(metaData.getColumnName(i), resultSet.getObject(i));
        }
        return json;
    }



    public void terminate() {
        try {
            if (conn != null) {
                conn.close();
            }
            clearSslProperties();
        } catch(SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * This method returns a connection to the db instance authenticated using IAM Database Authentication
     * @return
     * @throws Exception
     */
    private static Connection getDBConnectionUsingIam(String hostname, int port, String user) throws Exception {
        setSslProperties();
        String url = "jdbc:mysql://" + hostname + ":" + port + "/Proxily?autoReconnect=true&useSSL=false";
        System.out.println("jdbc url = " + url);
        //String pass = generateAuthToken(hostname, port, user);
        return DriverManager.getConnection(url, user, "password");
    }

    /**
     * This method sets the mysql connection properties which includes the IAM Database Authentication token
     * as the password. It also specifies that SSL verification is required.
     * @return
     */
    private static Properties setMySqlConnectionProperties(String hostname, int port, String user) {
        Properties mysqlConnectionProperties = new Properties();
        mysqlConnectionProperties.setProperty("verifyServerCertificate","false");
        mysqlConnectionProperties.setProperty("useSSL", "false");
        mysqlConnectionProperties.setProperty("user", user);
        mysqlConnectionProperties.setProperty("password","password");
        return mysqlConnectionProperties;
    }

    /**
     * This method generates the IAM Auth Token.
     * @return
     */
    private static String generateAuthToken(String hostname, int port, String user) {
        RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
                .credentials(new DefaultAWSCredentialsProviderChain()).region(Regions.getCurrentRegion()).build();
        return generator.getAuthToken(GetIamAuthTokenRequest.builder()
                .hostname(hostname).port(port).userName(user).build());
    }

    /**
     * This method sets the SSL properties which specify the key store file, its type and password:
     * @throws Exception
     */
    private static void setSslProperties() throws Exception {
        System.setProperty("javax.net.ssl.trustStore", createKeyStoreFile());
        System.setProperty("javax.net.ssl.trustStoreType", KEY_STORE_TYPE);
        System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_KEY_STORE_PASSWORD);
    }

    /**
     * This method returns the path of the Key Store File needed for the SSL verification during the IAM Database Authentication to
     * the db instance.
     * @return
     * @throws Exception
     */
    private static String createKeyStoreFile() throws Exception {
        return createKeyStoreFile(createCertificate()).getPath();
    }

    /**
     *  This method generates the SSL certificate
     * @return
     * @throws Exception
     */
    private static X509Certificate createCertificate() throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        URL url = new File(SSL_CERTIFICATE).toURI().toURL();
        if (url == null) {
            throw new Exception();
        }
        try (InputStream certInputStream = url.openStream()) {
            return (X509Certificate) certFactory.generateCertificate(certInputStream);
        }
    }

    /**
     * This method creates the Key Store File
     * @param rootX509Certificate - the SSL certificate to be stored in the KeyStore
     * @return
     * @throws Exception
     */
    private static File createKeyStoreFile(X509Certificate rootX509Certificate) throws Exception {
        File keyStoreFile = File.createTempFile(KEY_STORE_FILE_PREFIX, KEY_STORE_FILE_SUFFIX);
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile.getPath())) {
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
            ks.load(null);
            ks.setCertificateEntry("rootCaCertificate", rootX509Certificate);
            ks.store(fos, DEFAULT_KEY_STORE_PASSWORD.toCharArray());
        }
        return keyStoreFile;
    }

    /**
     * This method clears the SSL properties.
     */
    private static void clearSslProperties() {
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStoreType");
        System.clearProperty("javax.net.ssl.trustStorePassword");
    }

}