package io.jrest;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SessionUtil {
	
	/**
	 * Convert HttpSession in to HttpCookie object.
	 */
	public static HttpCookie toCookie(HttpSession session) {
		HttpCookie cookie = new HttpCookie(HttpSession.SESSION_NAME, session.getUUID().toString());
		cookie.setSecure(false);
		cookie.setHttpOnly(false);
		return cookie;
	}
	
	/**
	 * Get HttpSession from HttpCookie object. Normally, the uuid of the session is stored in the value of a cookie.
	 */
	public static HttpSession fromCookie(HttpCookie cookie) {
		return new HttpSession(cookie.getValue());
	}
	
	/**
	 * Serialize HttpSession to json string.
	 */
	public static String serialize(HttpSession session) {
		Map<String, Object> map = new HashMap<>();
		map.put("valid", session.valid);
		map.put("data", session.data);
		return RestUtil.convertToString(map);
	}
	
	/**
	 * Deserialize json string to a HttpSession. Required uuid.
	 */
	public static HttpSession deserialize(String uuid, String serializedHttpSession) {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = new ConvertToken<Map<String, Object>>() {
			@SuppressWarnings("rawtypes")
			@Override
			public Class getGenericClass() {
				return HashMap.class;
			}
		}.getValue(serializedHttpSession);
		if ( map == null )
			return null;
		
		boolean hasValid = map.containsKey("valid");
		if ( !hasValid ) 
			return null;
		
		boolean valid = map.get("valid") instanceof Boolean ? (Boolean)map.get("valid") : false;
		
		boolean hasData = map.containsKey("data");
		if ( !hasData )
			return null;
		
		@SuppressWarnings("unchecked")
		Map<String, Object> data = map.get("data") instanceof Map ? (Map<String, Object>)map.get("data") : null;
		if ( data == null )
			return null;
		
		HttpSession session = new HttpSession(uuid);
		session.valid = valid;
		session.data = data;
		
		return session;
	}
	
	/**
	 * Serialize list of HttpSessions to json.
	 */
	public static String serializeAll(List<HttpSession> sessions) {
		Map<String, Object> map = new HashMap<>();
		for (HttpSession session : sessions) {
			Map<String, Object> map2 = new HashMap<>();
			map2.put("valid", session.valid);
			map2.put("data", session.data);
			
			map.put(session.getUUID().toString(), map2);
		}
		
		return RestUtil.convertToString(map);
	}
	
	/**
	 * Deserialize list of serialized HttpSessions back into a List of HttpSessions.
	 */
	public static List<HttpSession> deserializeAll(String serializedJson) {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = new ConvertToken<Map<String, Object>>() {
			@SuppressWarnings("rawtypes")
			@Override
			public Class getGenericClass() {
				return HashMap.class;
			}
		}.getValue(serializedJson);
		if ( map == null )
			return new ArrayList<>();
		
		List<HttpSession> results = new ArrayList<>();
		for (Entry<String, Object> set : map.entrySet()) {
			String uuid = set.getKey();
			String sessionJson = RestUtil.convertToString(set.getValue()); // TODO write this in a not horrible way. This is just the easiest given the other implementations.
			HttpSession session = deserialize(uuid, sessionJson);
			if ( session == null )
				continue;
			
			results.add(session);
		}
		
		return results;
	}
	
	private static abstract class ConvertToken<T> {
		@SuppressWarnings("unchecked")
		public T getValue(String input) {
			return (T) RestUtil.convertToObject(input, getGenericClass());
		}
		
		public abstract Class<T> getGenericClass();
	}
}
