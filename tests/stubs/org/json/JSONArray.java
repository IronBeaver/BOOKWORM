package org.json;
import java.util.*;
public class JSONArray {
    final List<?> list; JSONArray(Object value){list=(List<?>)value;}
    public int length(){return list.size();}
    public JSONObject getJSONObject(int i)throws JSONException {if(i<0||i>=list.size()||!(list.get(i) instanceof Map))throw new JSONException("Not object");return new JSONObject(list.get(i));}
    public String optString(int i){return i<0||i>=list.size()||list.get(i)==null?"":String.valueOf(list.get(i));}
}
