package com.socrata;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * Represents a Socrata user.
 * 
 * @author aiden.scandella@socrata.com
 */
public class User extends ApiBase {
    private String username;

    /**
     * Default constructor. Associates a username with their Socrata profile
     * @param username The name or UID of the user
     */
    public User(String username) {
        super();

        this.username = username;
    }

    /**
     * Gets all the datasets belonging to a user
     * @return A list of Datasets belonging to this user
     */
    public List<Dataset> datasets() {
        HttpGet request = new HttpGet(httpBase() + "/users/" + username + "/views.json");
        JsonPayload response = performRequest(request);
        if ( isErroneous(response) ) {
            log(Level.SEVERE, "Could not fetch datasets for user '" + username + "'", null);
            return null;
        }

        JSONArray jsonSets = response.getArray();
        List<Dataset> sets = new LinkedList<Dataset>();
        
        for( int i = 0; i < jsonSets.length(); i++ ) {
            Dataset set = new Dataset(this.properties);
            try {
                JSONObject jsonSet = jsonSets.getJSONObject(i);
                String setUID = jsonSet.getString("id");
                set.attach(setUID);
                sets.add(set);
            }
            catch ( JSONException ex) {
                log(Level.SEVERE, "Could not grab JSON object out of array at index: " + i, null);
            }
        }

        return sets;
    }
    

}
