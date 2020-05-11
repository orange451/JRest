package io.jrest;

public class ResponseEntity<T> extends HttpEntity<T> {

	private HttpStatus status;
	
	public ResponseEntity(HttpStatus status) {
		this(status, null, null);
	}

	public ResponseEntity(HttpStatus status, T body) {
		this(status, null, body);
	}

	public ResponseEntity(HttpStatus status, HttpHeaders headers) {
		this(status, headers, null);
	}

	public ResponseEntity(HttpStatus status, HttpHeaders headers, T body) {
		super(headers, body);
		this.status = status;
	}
	
	public HttpStatus getStatus() {
		return this.status;
	}
}
