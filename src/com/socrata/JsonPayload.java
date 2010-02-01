package com.socrata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Convenience class to avoid crazy parsing issues when dealing with
 * httpclient and trying to convert the response to usable org.json JSON
 * @author aiden.scandella@socrata.com
 */
public class JsonPayload {
    private JSONObject  jsonObject;
    private JSONArray   jsonArray;
    private String      stringResponse;

    /**
     * Class constructor from apache commons response object
     * @param response  what you got from your httpclient.execute()
     */
    public JsonPayload (HttpResponse response) {
        HttpEntity entity = response.getEntity();
        InputStream stream;
        
        try {
            stream = entity.getContent();
        }
        catch (IOException ex) {
            Logger.getLogger(JsonPayload.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        catch (IllegalStateException ex) {
            Logger.getLogger(JsonPayload.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        InputStreamReader responseReader = new InputStreamReader(stream);
        extractJson(responseReader);
        if ( stream != null ) {
            try {
                stream.close();
            }
            catch (Exception ex) {
                Logger.getLogger(JsonPayload.class.getName()).log(Level.SEVERE,
                        "Could not close entity stream, subsequent requests will fail", ex);
                try {
                    entity.consumeContent();
                }
                catch (Exception exor) {
                    // Ignore
                }
            }
        }
    }

    private void extractJson(Reader reader) {
        // JSONTokener tokener = new JSONTokener(reader);
        String jsonString = readerToString(reader);
        JSONTokener tokener = new JSONTokener(jsonString);
        if ( jsonString.length() > 0 && jsonString.charAt(0) == '[') {
            try {
                jsonArray = new JSONArray(tokener);
            }
            catch (Exception exor) {
                // Ignore...
            }
        } else if ( jsonString.length() > 0 && jsonString.charAt(0) == '{' ) {
            // First try to cast it to a Json Object, i.e. {...}
            try {
                jsonObject = new JSONObject(tokener);
            }
            catch (Exception ex) {
                // Ignore...
            }
        } else {
            stringResponse = jsonString;
            if ( stringResponse.length() > 0 ) {
                Logger.getLogger(JsonPayload.class.getName()).log(Level.WARNING,
                         "Failed to convert JSON to object or array");
            }
        }
    }

    private String readerToString(Reader r) {
        String lineBuffer;
        StringBuffer buff = new StringBuffer();
        BufferedReader br = new BufferedReader(r);
        try {
            lineBuffer = br.readLine();
            while ( lineBuffer != null ) {
                buff.append(lineBuffer);
                lineBuffer = br.readLine();
            }
        }
        catch (Exception e) {
            Logger.getLogger(JsonPayload.class.getName()).log(Level.WARNING,
                    "Failed to convert Reader to String", e);
        }
        return buff.toString();
    }

    /**
     * If available, returns the object associated with this envelope
     * @return the json object version of the response
     */
    public JSONObject getObject() {
        return this.jsonObject;
    }

    /**
     * If available, returns the array of objects associated with this envelope
     * @return the json array of json objects
     */
    public JSONArray getArray() {
        return this.jsonArray;
    }

    /**
     * Returns the string representation if no JSON was present
     * @return the textual response from the httpclient
     */
    public String getResponse() {
        return this.stringResponse;
    }

    @Override
    public String toString() {
        StringBuffer us = new StringBuffer();
        
        if ( getResponse() != null && !getResponse().isEmpty()) {
            us.append("String Response: \"" + getResponse() + "\", ");
        }
        try {
            if ( getArray() != null ) {
                String arrayRep = getArray().toString(4);
                us.append("Array: " + arrayRep);
            }
            if ( getObject() != null ) {
                String objRep = getObject().toString(4);
                us.append("Object: " + objRep);

            }
        } catch ( Exception e ) {
            // Ignore
        }
        
        return us.toString();
    }
}
