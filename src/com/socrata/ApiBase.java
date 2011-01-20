package com.socrata;

/*

Copyright (c) 2010 Socrata.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 */

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class under which all Socrata classes inherit.
 *
 * @author aiden.scandella@socrata.com
 */
public abstract class ApiBase {
    protected DefaultHttpClient httpClient;
    protected HttpContext httpContext;
    protected HttpHost httpHost;
    protected ResourceBundle properties;
    protected String              username, password, appToken;

    protected List<BatchRequest> batchQueue;

    /**
     * Default class constructor.
     */
    public ApiBase() {
        loadProperties();
        finishConstruction();
    }

    /**
     * Copy settings from an existing object
     * @param properties Specifies credentials and hostnames
     */
    public ApiBase(ResourceBundle properties) {
        this.properties = properties;
        this.httpClient = new DefaultHttpClient();
        finishConstruction();
    }

    private void finishConstruction() {
        // Store these because we need them for some specialized calls
        this.username = properties.getString("username");
        this.password = properties.getString("password");
        this.appToken = properties.getString("app_token");

        batchQueue = new ArrayList<BatchRequest>();

        setupBasicAuthentication();

        httpHost = new HttpHost(properties.getString("hostname"),
                Integer.valueOf(properties.getString("hostport")));

        httpContext = new BasicHttpContext();
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
     * Empty out the batchQueue, sending stored data back to Socrata servers
     * @return success or failure
     */
    public boolean sendBatchRequest() {
        if ( batchQueue == null || batchQueue.size() == 0 ) {
            log(Level.WARNING, "No batch requests in queue, ignoring call to sendBatchRequest" , null);
            return false;
        }
        Collection batches = new ArrayList<Map>();
        for( BatchRequest b : batchQueue ) {
            batches.add(b.data());
        }

        JSONObject bodyObject = new JSONObject();
        try {
            bodyObject.put("requests", batches);
        }
        catch ( JSONException ex ) {
            log(Level.SEVERE, "Could not convert array of batch requests to JSON", ex);
            return false;
        }

        HttpPost request = new HttpPost(httpBase() + "/batches");
        try {
            request.setEntity(new StringEntity(bodyObject.toString()));
            log(Level.INFO, bodyObject.toString());
        }
        catch ( UnsupportedEncodingException ex ) {
            log(Level.SEVERE, "Could not encode JSON data into HTTP entity", ex);
            return false;
        }

        JsonPayload response = performRequest(request);
        if ( !isErroneous(response) ) {
            log(Level.INFO, "Completed batch request, clearing out queue of " +
                    batchQueue.size() + " entries.");
            batchQueue.clear();
            return true;
        }

        return false;
    }

    /**
     * Performs a generic request against Socrata API servers
     * @param request Apache HttpRequest object (e.g. HttpPost, HttpGet)
     * @return JSON array representation of the response
     */
    protected JsonPayload performRequest(HttpRequestBase request) {
        HttpResponse response;
        HttpEntity entity;

        request.addHeader("X-App-Token", this.appToken);
        try {
            response = httpClient.execute(httpHost, request, httpContext);

            if( response.getStatusLine().getStatusCode() != 200 ) {
                log(java.util.logging.Level.SEVERE, "Got status " +
                        response.getStatusLine().getStatusCode() + ": " +
                        response.getStatusLine().toString() +
                        " while performing request on " + request.getURI(), null);
                return null;
            }

            return new JsonPayload(response);
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

        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();

        httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.AUTH_CACHE, basicAuth);


        httpClient.setCredentialsProvider(credProvider);
    }


    /**
     * Returns the base URL for http requests
     * @return the base URL for http requests
     */
    public String httpBase() {
        return "http://" + properties.getString("hostname") +
                ":" + properties.getString("hostport");
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
     * @return true if response contains errors
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
