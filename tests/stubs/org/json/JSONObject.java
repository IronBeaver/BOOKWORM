package org.json;
import java.util.*;
/** Host-only adapter to Gradle's already-installed JSON parser, not an app dependency. */
public class JSONObject {
    final Map<String,Object> map;
    @SuppressWarnings("unchecked") public JSONObject(String json) throws JSONException {
        try {Object value=new groovy.json.JsonSlurper().parseText(json);if(!(value instanceof Map))throw new IllegalArgumentException();map=(Map<String,Object>)value;}
        catch(RuntimeException bad){throw new JSONException("Invalid JSON");}
    }
    @SuppressWarnings("unchecked") JSONObject(Object value) {map=(Map<String,Object>)value;}
    public Object opt(String key) {return map.get(key);}
    public String optString(String key) {Object v=opt(key);return v==null?"":String.valueOf(v);}
    public int optInt(String key) {try{return Integer.parseInt(optString(key));}catch(Exception bad){return 0;}}
    public int getInt(String key)throws JSONException {try{return Integer.parseInt(optString(key));}catch(Exception bad){throw new JSONException("Not integer");}}
    public JSONArray optJSONArray(String key) {return opt(key) instanceof List?new JSONArray(opt(key)):null;}
    public JSONArray getJSONArray(String key)throws JSONException {JSONArray a=optJSONArray(key);if(a==null)throw new JSONException("Not array");return a;}
    public JSONObject optJSONObject(String key) {return opt(key) instanceof Map?new JSONObject(opt(key)):null;}
}
