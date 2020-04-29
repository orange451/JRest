package jrest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class RestUtil {
	protected final static Gson gson;
	
	static {
		gson = new GsonBuilder().serializeNulls().create();
	}
	
	@SuppressWarnings("unchecked")
	protected static <T> T convertObject(String bodyString, T type) {
		if ( bodyString == null || bodyString.length() == 0 )
			return (T)null;

		Class<?> c = (Class<?>)type;
		
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
		
		// Convert to String
		if ( String.class.isAssignableFrom(c) ) {
			return (T) bodyString.toString();
		}
		
		// Try to parse DTO as fallback
		try {
			return (T) gson.fromJson(bodyString, c);
		} catch(Exception e) {
			return null;
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
