package jrest;

import java.net.HttpCookie;
import java.util.List;

public class HttpEntity<T> {
	private HttpHeaders headers;
	
	private T body;
	
	protected List<HttpCookie> cookies;
	
	public HttpEntity(HttpHeaders headers, T body) {
		this.headers = headers;
		this.body = body;
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
}
