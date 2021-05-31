package io.jrest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest<P> extends HttpEntity<P> {
	
	private HttpMethod method;
	
	protected URI uri;

	protected Map<String, String> urlParams;
	
	public HttpRequest(HttpHeaders headers) {
		this(HttpMethod.GET, headers);
	}
	
	public HttpRequest(HttpMethod method, HttpHeaders headers) {
		this(method, headers, null);
	}
	
	public HttpRequest(HttpMethod method, HttpHeaders headers, P body) {
		super(headers, body);
		this.method = method;
		this.urlParams = new HashMap<>();
	}
	
	/**
	 * HTTP Method used to invoke a HTTP Request
	 */
	public HttpMethod getMethod() {
		return this.method;
	}
	
	/**
	 * HTTP Method string used to invoke a HTTP Request
	 */
	public String getMethodValue() {
		return this.method.toString();
	}
	
	/**
	 * URI of this request.
	 */
	public URI getURI() {
		return this.uri;
	}
	
	@Override
	public String toString() {
		return "HttpRequest["+uri+", "+method+"]";
	}

	/**
	 * Parameters included in the URI of this HTTP Request.
	 */
	public Map<String,String> getUrlParameters() {
		return urlParams;
	}
}
