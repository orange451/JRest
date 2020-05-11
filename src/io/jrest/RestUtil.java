package io.jrest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class RestUtil {
	private static Gson gson;
	
	private static boolean canUseGson;
	
    private static ScriptEngine engine;
	
	static {
		try {
			gson = new GsonBuilder().serializeNulls().create();
			canUseGson = true;
		} catch(NoClassDefFoundError e) {
			System.err.println("Could not locate Gson dependency, will not serialize Java classes. Using Nashorn engine as fallback Map/List serializer.");
			
	        ScriptEngineManager sem = new ScriptEngineManager();
	        engine = sem.getEngineByName("javascript");
		}
	}
	
	/**
	 * Attempts to serialize an object (Map, List, POJO, String) to a string.
	 * @param object
	 * @return
	 */
	protected static String convertSoString(Object object) {
		if (object instanceof String)
			return object.toString();
		
		if ( canUseGson )
			return RestUtil.gson.toJson(object);
		
		// Oh boy manual json serialization...
		if ( object instanceof Map || object instanceof List )
			return serializeJson(object);
		
		// Fallback
		return object.toString();
	}
	
	/**
	 * Manual json serialization. Only used if Gson is not available.
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static String serializeJson(Object object) {
		
		if ( object instanceof Map ) {
			String str = "{";
			boolean first = true;
			for (Map.Entry<Object,Object> entry : ((Map<Object, Object>)object).entrySet()) {
				if ( !first )
					str += ", ";
				str += getSerializedJSONValue(entry.getKey().toString()) + ": " + getSerializedJSONValue(entry.getValue());
				first = false;
			}
			str += "}";
			return str;
		} else if ( object instanceof List ) {
			String str = "[";
			boolean first = true;
			for (Object obj : (List<Object>)object) {
				if ( !first )
					str += ", ";
				str += getSerializedJSONValue(obj);
				first = false;
			}
			str += "]";
			return str;
		}
		
		return new String();
	}
	
	private static String getSerializedJSONValue(Object value) {
		if ( value == null )
			return "null";
		else if ( value instanceof List )
			return serializeJson(value);
		else if ( value instanceof Map )
			return serializeJson(value);
		else if ( value instanceof Number || value instanceof Boolean )
			return ((Number)value).toString();
		else if ( value instanceof String )
			return "\"" + value.toString() + "\"";
		
		// Cant be serialized
		return "null";
	}

	@SuppressWarnings("unchecked")
	protected static <T> T convertObject(String bodyString, T type) {
		if ( bodyString == null || bodyString.length() == 0 )
			return (T)null;

		Class<?> c = (Class<?>)type;
		
		if ( canUseGson ) {
			// Convert to gson tree
			if ( JsonObject.class.isAssignableFrom(c) ) {
				Type empMapType = new TypeToken<Map<String, Object>>() {}.getType();
				return (T) gson.toJsonTree(gson.fromJson(bodyString, empMapType)).getAsJsonObject();
			}
			
			// json array
			if ( JsonArray.class.isAssignableFrom(c) ) {
				Type empMapType = new TypeToken<List<Object>>() {}.getType();
				Object obj = gson.fromJson(bodyString, empMapType);
				return (T) gson.toJsonTree(obj).getAsJsonArray();
			}
			
			// Convert to map
			if ( Map.class.isAssignableFrom(c) ) {
				Type empMapType = new TypeToken<Map<String, Object>>() {}.getType();
				return gson.fromJson(bodyString, empMapType);
			}
			
			// Convert to list
			if ( List.class.isAssignableFrom(c) ) {
				Type empMapType = new TypeToken<List<Object>>() {}.getType();
				return gson.fromJson(bodyString, empMapType);
			}
		} else {
			// Super ugly hack using Javax Nashorn js library. We can only reliably get Maps or Lists.
	        String script = "Java.asJSONCompatible(" + bodyString + ")";
	        try {
				Object result = engine.eval(script);
				if ( result instanceof Map )
					return (T)((Map<?, ?>)result);
				if ( result instanceof List )
					return (T)((List<?>)result);
			} catch (ScriptException e) {
				System.err.println("Failed to parse " + script);
				e.printStackTrace();
			}
		}
		
		// Convert to String
		if ( String.class.isAssignableFrom(c) ) {
			return (T) bodyString.toString();
		}
		
		// Try to parse DTO as fallback
		if ( canUseGson ) {
			try {
				return (T) gson.fromJson(bodyString, c);
			} catch(Exception e) {
				return null;
			}
		} else {
			return (T) bodyString.toString();
		}
	}
	
    protected static byte[] readAll(InputStream inputStream) throws IOException {
		long TIMEOUT = System.currentTimeMillis()+2000;
		
		// Wait until ready
		BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
		while(bufferedInput.available() == 0) {
			if ( System.currentTimeMillis() > TIMEOUT ) {
				return null;
			}
		}
		
		// Ready until empty
		byte[] data = new byte[bufferedInput.available()];
		bufferedInput.read(data);
		return data;
    }
}
