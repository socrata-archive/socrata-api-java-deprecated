package com.socrata;

import java.util.Map;
import java.util.HashMap;

/**
 * Holds the data for a future batch request
 *
 * @author aiden.scandella@socrata.com
 */

public class BatchRequest {
    private Map data;

    /**
     * Simple constructor, sets up the map of data to be converted to JSON
     * @param requestType One of: GET, POST, PUT, DELETE, etc.
     * @param url The non-rooted url to hit with this request
     * @param body The payload, usually a String version of JSON data
     */
    public BatchRequest(String requestType, String url, String body) {
        this.data = new HashMap<String, String>();
        data.put("url", url);
        data.put("requestType", requestType);
        data.put("body", body);
    }

    /**
     * Get the data associated with this batch request
     * @return the map of key/value pairs
     */
    public Map data() {
        return this.data;
    }

}
