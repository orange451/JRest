package io.jrest;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HttpSession {
	
	public static final String SESSION_NAME = "JRESTSESSID";
	
	/**
	 * Unique key to identify this session
	 */
	private UUID uuid;
	
	/**
	 * Data held in this session
	 */
	private Map<String, Object> data;
	
	/**
	 * Describes if the session is still valid
	 */
	private boolean valid;
	
	public HttpSession(UUID uuid) {
		this.data = new HashMap<>();
		this.uuid = uuid;
		this.valid = true;
	}
	
	public HttpSession() {
		this(UUID.randomUUID());
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	public void invalidate() {
		valid = false;
	}
	
	public boolean isValid() {
		return this.valid;
	}
	
	public void put(String key, Object value) {
		this.data.put(key, value);
	}
	
	public Object get(String key) {
		return this.data.get(key);
	}
	
	public HttpCookie toCookie() {
		HttpCookie cookie = new HttpCookie(SESSION_NAME, uuid.toString());
		cookie.setSecure(false);
		cookie.setHttpOnly(false);
		return cookie;
	}
}
