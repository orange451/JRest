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
	
	public T getBody() {
		return this.body;
	}
	
	public HttpHeaders getHeaders() {
		return this.headers;
	}
	
	public List<HttpCookie> getCookies() {
		return this.cookies;
	}
	
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
