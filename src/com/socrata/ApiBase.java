package com.socrata;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class under which all Socrata classes inherit.
 * 
 * @author aiden.scandella@socrata.com
 */
public abstract class ApiBase {
    protected DefaultHttpClient   httpClient;
    protected ResourceBundle      properties;
    protected String              username, password;
    

    /**
     * Default class constructor.
     * 
     * @return A new Socrata API object
     */
    public ApiBase() {
        loadProperties();
        // Store these because we need them for some specialized calls
        this.username = properties.getString("username");
        this.password = properties.getString("password");

        setupBasicAuthentication();
    }

    /**
     * Class constructor with user-specified username/password.
     *
     * @param username  the user account to connect with
     * @param password  the password to connect with
     * @return  a new Socrata API object with specified credentials
     */
    public ApiBase(String username, String password) {
        loadProperties();
        
        this.username = username;
        this.password = password;

        setupBasicAuthentication();
    }

    /**
     * Performs a generic request against Socrata API servers
     * @param Apache HttpRequest object (e.g. HttpPost, HttpGet)
     * @return JSON array representation of the response
     */
    protected JsonPayload performRequest(HttpRequestBase request) {
        HttpResponse response;
         HttpEntity entity;
        try {
            response = httpClient.execute(request);
            
            if( response.getStatusLine().getStatusCode() != 200 ) {
                log(java.util.logging.Level.SEVERE, "Got status " +
                        response.getStatusLine().getStatusCode() + ": " +
                        response.getStatusLine().toString() +
                        " while performing request on " + request.getURI(), null);
                return null;
            }
            
            // entity = response.getEntity();
            // InputStream stream = entity.getContent();
            
            // InputStreamReader responseReader = new InputStreamReader(stream);

            JsonPayload payload = new JsonPayload(response);

            // JSONObject jsonResponse = new JSONObject(new JSONTokener(responseReader));
            
            // System.err.print(jsonResponse.toString(4));

            // stream.close();
            return payload;
            // return jsonResponse;
        }
        catch (Exception ex) {
            log(Level.SEVERE, "Error caught trying to perform HTTP request", ex);
            return null;
        } 
    }

    /**
     * Loads necessary connection details from disk.
     */
    private void loadProperties() {
        this.properties = ResourceBundle.getBundle("com.socrata.resources");
        this.httpClient = new DefaultHttpClient();
    }

    /**
     * Sets up http authentication (BASIC) for default requests
     */
    private void setupBasicAuthentication() {
        Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
        CredentialsProvider credProvider = new BasicCredentialsProvider();

        credProvider.setCredentials(AuthScope.ANY, defaultcreds);
        httpClient.setCredentialsProvider(credProvider);
    }


    /**
     * Returns the base URL for http requests
     * @return the base URL for http requests
     */
    public String httpBase() {
        return properties.getString("api_host");
    }

    /**
     * Sends a message to the logger
     * @param l  the level of severity
     * @param message  a message to put in the logs
     * @param exception  an exception, if applicable, that triggered this log entry
     */
    protected void log(Level l, String message, Exception exception) {
        Logger.getLogger(ApiBase.class.getName()).log(l, message, exception);
    }

    /**
     * Sends a message to the logger
     * @param l  the level of severity
     * @param message  a message to put in the logs
     */
    protected void log(Level l, String message) {
        log(l, message, null);
    }

    /**
     * Inspects the JSON payload returned from the API server for error messages
     * @param response the response
     * @return true if resoponse contains errors
     */
    protected boolean isErroneous(JsonPayload response) {
        if ( response == null ) {
            return true;
        }
        // If the message couldn't be parsed into JSON
        if( response.getResponse() != null && !response.getResponse().isEmpty()) {
            log(Level.WARNING, "Non-JSON response: " + response.getResponse(), null);
            return true;
        } else if ( response.getObject() != null ) {
            try {
                if (response.getObject().has("error")) {
                    log(Level.SEVERE, "Error in server response: " +
                            response.getObject().getString("error"), null);
                    return true;
                }
            } catch (JSONException ex) {
                Logger.getLogger(ApiBase.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if ( response.getArray() != null ) {
            // An array means it's not erroneous
            return false;
        }
        // No response means no error
        return false;
    }
}
