package io.jrest;

class HttpResponse<T> extends HttpEntity<T> {
	
	private HttpStatus status;

	public HttpResponse(HttpHeaders headers) {
		this(HttpStatus.OK, headers);
	}

	public HttpResponse(HttpStatus status) {
		this(status, new HttpHeaders());
	}
	
	public HttpResponse(HttpStatus method, HttpHeaders headers) {
		this(method, headers, null);
	}
	
	public HttpResponse(HttpStatus status, HttpHeaders headers, T body) {
		super(headers, body);
		this.status = status;
	}
	
	public HttpStatus getStatus() {
		return this.status;
	}
	
	@Override
	public String toString() {
		return "HttpResponse["+status+"]";
	}
}
