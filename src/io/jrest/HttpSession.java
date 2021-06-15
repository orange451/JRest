package io.jrest;

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
	protected Map<String, Object> data;
	
	/**
	 * Describes if the session is still valid
	 */
	protected boolean valid;
	
	public HttpSession(UUID uuid) {
		this.data = new HashMap<>();
		this.uuid = uuid;
		this.valid = true;
	}
	
	public HttpSession(String uuid) {
		this(UUID.fromString(uuid));
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
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		s.append("HttpSession [");
		s.append("UUID=");
		s.append(uuid);
		s.append(", Valid=");
		s.append(valid);
		s.append(", Data=");
		s.append(RestUtil.convertToString(data));
		s.append("]");
		
		return s.toString();
	}
}
