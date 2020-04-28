package jrest;

public class HttpEntity<T> {
	private HttpHeaders headers;
	
	private T body;
	
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
}
