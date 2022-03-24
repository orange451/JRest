package io.jrest;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class MarshallerGson extends Marshaller {

	private com.google.gson.Gson gson;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T parse(String body, T type) {
		if ( gson == null )
			gson = new com.google.gson.GsonBuilder().serializeNulls().setLenient().create();
		
		if ( type == null || body == null )
			return null;
		
		// Kinda ugly PLS FIX
		if ( body.length() > 1024 ) {
			body = body.trim();
		}
		
		Class<?> c = (Class<?>) type;
		
		// Convert to user specific DTO object
		if ( !String.class.isAssignableFrom(c) ) {
			try {
				return (T) gson.fromJson(body, c);
			} catch(Exception e) {
				//
			}
		}

		// Convert to gson tree
		if (com.google.gson.JsonObject.class.isAssignableFrom(c)) {
			return (T) gson.fromJson(body, c);
		}

		// json array
		if (com.google.gson.JsonArray.class.isAssignableFrom(c)) {
			Type empMapType = new com.google.gson.reflect.TypeToken<List<Object>>() {}.getType();
			Object obj = gson.fromJson(body, empMapType);
			return (T) gson.toJsonTree(obj).getAsJsonArray();
		}

		// Convert to map
		if (Map.class.isAssignableFrom(c)) {
			Type empMapType = new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType();
			return gson.fromJson(body, empMapType);
		}

		// Convert to list
		if (List.class.isAssignableFrom(c)) {
			Type empMapType = new com.google.gson.reflect.TypeToken<List<Object>>() {}.getType();
			return gson.fromJson(body, empMapType);
		}
		
		return null;
	}

	@Override
	public String stringify(Object body) {
		return gson.toJson(body);
	}

}
