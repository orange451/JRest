package io.jrest;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

public class HttpEntity<T> {
	private HttpHeaders headers;
	
	private T body;
	
	protected List<HttpCookie> cookies;
	
	public HttpEntity(HttpHeaders headers, T body) {
		this.headers = headers;
		this.body = body;
		this.cookies = new ArrayList<>();
	}
	
	/**
	 * Data that represents the body of this http entity.
	 */
	public T getBody() {
		return this.body;
	}
	
	/**
	 * HTTP Headers object.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}
	
	/**
	 * HTTP Cookies attached to this entity.
	 */
	public List<HttpCookie> getCookies() {
		return this.cookies;
	}
	
	/**
	 * Search for cookie matching a given name attached to this HTTP entity.
	 */
	public HttpCookie getCookie(String key) {
		if ( cookies == null || key == null )
			return null;
		
		for (HttpCookie cookie : cookies) {
			if ( cookie.getName().equals(key) )
				return cookie;
		}
		
		return null;
	}
}
