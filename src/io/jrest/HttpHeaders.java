package io.jrest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpHeaders {
	public static final String USER_AGENT = "User-Agent";
	
	public static final String ACCEPT = "Accept";
	
	public static final String HOST = "Host";
	
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	
	public static final String CONTENT_ENCODING = "Content-Encoding";
	
	public static final String CONTENT_TYPE = "Content-Type";
	
	public static final String AUTHORIZATION = "Authorization";

	public static final String KEEP_ALIVE = "Keep-Alive";

	public static final String SERVER = "Server";

	public static final String CONNECTION = "Connection";
	
	private Map<String, String> data;
	
	public HttpHeaders() {
		this.data = new HashMap<>();
		
		this.setAccept(MediaType.ALL);
		this.setAcceptEncoding("gzip, deflate");
		this.setContentEncoding("gzip");
		this.setContentType(MediaType.TEXT_PLAIN);
		this.put("Cache-control", "no-cache");
	}
	
	public HttpHeaders put(String key, String value) {
		this.data.put(key, value);
		return this;
	}
	
	public String get(String key) {
		return this.data.get(key);
	}
	
	public HttpHeaders setContentType(MediaType type) {
		this.put(CONTENT_TYPE, type.toString());
		return this;
	}
	
	public String getContentType() {
		return this.get(CONTENT_TYPE);
	}
	
	public HttpHeaders setAuthorization(String authorization) {
		this.put(AUTHORIZATION, authorization);
		return this;
	}
	
	public String getAuthorization() {
		return this.get(AUTHORIZATION);
	}
	
	public HttpHeaders setUserAgent(String agent) {
		this.put(USER_AGENT, agent);
		return this;
	}
	
	public String getUserAgent() {
		return this.get(USER_AGENT);
	}
	
	public HttpHeaders setAccept(MediaType data) {
		this.put(ACCEPT, data.toString());
		return this;
	}
	
	public String getAccept() {
		return this.get(ACCEPT);
	}
	
	public HttpHeaders setHost(String host) {
		this.put(HOST, host);
		return this;
	}
	
	public String getHost() {
		return this.get(HOST);
	}
	
	public HttpHeaders setAcceptEncoding(String encoding) {
		this.put(ACCEPT_ENCODING, encoding);
		return this;
	}
	
	public String getAcceptEncoding() {
		return this.get(ACCEPT_ENCODING);
	}
	
	public HttpHeaders setContentEncoding(String encoding) {
		this.put(CONTENT_ENCODING, encoding);
		return this;
	}
	
	public String getContentEncoding() {
		return this.get(CONTENT_ENCODING);
	}

	public Set<Entry<String, String>> entrySet() {
		return this.data.entrySet();
	}
}
