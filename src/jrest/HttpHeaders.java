package jrest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpHeaders {
	public static final String USER_AGENT = "User-Agent";
	
	public static final String ACCEPT = "Accept";
	
	public static final String HOST = "Host";
	
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	
	public static final String CONTENT_TYPE = "Content-Type";
	
	public static final String AUTHORIZATION = "Authorization";
	
	private Map<String, String> data;
	
	public HttpHeaders() {
		this.data = new HashMap<>();
		
		this.setAccept(MediaType.ALL);
		this.setAcceptEncoding("gzip, deflate, br");
		this.setContentType(MediaType.TEXT_PLAIN);
		this.put("Cache-control", "no-cache");
	}
	
	public void put(String key, String value) {
		this.data.put(key, value);
	}
	
	public String get(String key) {
		return this.data.get(key);
	}
	
	public void setContentType(MediaType type) {
		this.put(CONTENT_TYPE, type.toString());
	}
	
	public String getContentType() {
		return this.get(CONTENT_TYPE);
	}
	
	public void setAuthorization(String authorization) {
		this.put(AUTHORIZATION, authorization);
	}
	
	public String getAuthorization() {
		return this.get(AUTHORIZATION);
	}
	
	public void setUserAgent(String agent) {
		this.put(USER_AGENT, agent);
	}
	
	public String getUserAgent() {
		return this.get(USER_AGENT);
	}
	
	public void setAccept(MediaType data) {
		this.put(ACCEPT, data.toString());
	}
	
	public String getAccept() {
		return this.get(ACCEPT);
	}
	
	public void setHost(String host) {
		this.put(HOST, host);
	}
	
	public String getHost() {
		return this.get(HOST);
	}
	
	public void setAcceptEncoding(String encoding) {
		this.put(ACCEPT_ENCODING, encoding);
	}
	
	public String getAcceptEncoding() {
		return this.get(ACCEPT_ENCODING);
	}

	public Set<Entry<String, String>> entrySet() {
		return this.data.entrySet();
	}
}
