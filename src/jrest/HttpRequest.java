package jrest;

import java.net.URI;

public class HttpRequest<T> extends HttpEntity<T> {
	
	private HttpMethod method;
	
	protected URI uri;

	public HttpRequest(HttpHeaders headers) {
		this(HttpMethod.GET, headers);
	}
	
	public HttpRequest(HttpMethod method, HttpHeaders headers) {
		this(method, headers, null);
	}
	
	public HttpRequest(HttpMethod method, HttpHeaders headers, T body) {
		super(headers, body);
		this.method = method;
	}
	
	public HttpMethod getMethod() {
		return this.method;
	}
	
	public String getMethodValue() {
		return this.method.toString();
	}
	
	public URI getURI() {
		return this.uri;
	}
	
	@Override
	public String toString() {
		return "HttpRequest["+uri+", "+method+"]";
	}
}
