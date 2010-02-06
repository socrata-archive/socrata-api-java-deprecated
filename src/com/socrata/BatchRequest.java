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
