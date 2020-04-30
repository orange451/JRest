package jrest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

public class RequestEntity<T> extends HttpEntity<T> {

	private HttpMethod method;
	
	public RequestEntity() {
		this(HttpMethod.GET, (T)null);
	}
	
	public RequestEntity(HttpMethod method) {
		this(HttpMethod.GET, (T)null);
	}

	public RequestEntity(HttpMethod method, T body) {
		this(method, new HttpHeaders(), body);
	}

	public RequestEntity(HttpMethod method, HttpHeaders headers) {
		this(method, headers, null);
	}

	public RequestEntity(HttpMethod method, HttpHeaders headers, T body) {
		super(headers, body);
		this.method = method;
	}
	
	public HttpMethod getMethod() {
		return this.method;
	}
	
	public <P, Q> void exchangeAsync(String url, Class<Q> type, AsyncResponse<Q> response) throws MalformedURLException {
		this.exchangeAsync(new URL(url), type, response);
	}
	
	public <P, Q> void exchangeAsync(URL url, Class<Q> type, AsyncResponse<Q> response) {
		new Thread(()->{
			try {
				response.response(exchange(url, type));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public ResponseEntity<T> exchange(String url, Class<T> type) throws MalformedURLException,IOException {
		return this.exchange(new URL(url), type);
	}
	
	public <P, Q> ResponseEntity<Q> exchange(URL url, Class<Q> type) throws IOException {
		// Connect to endpoint
		try {
			
			// Connect
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoInput(true);
			if ( this.getMethod().equals(HttpMethod.POST) )
				con.setDoOutput(true);
			con.setRequestMethod(this.getMethod().toString());

			// Hidden headers
			if ( this.getHeaders().get("Host") == null )
				this.getHeaders().put("Host", url.getHost());
			
        	// Write headers
			for (Entry<String, String> entry : this.getHeaders().entrySet()) {
				try { con.setRequestProperty(entry.getKey(), entry.getValue()); } catch( Exception e) {}
			}
            
            // Get usable body
        	String body = null;
        	if ( getBody() != null )
        		body = getBody().toString();
        	else
        		body = new String();
            
        	// Write body
			if ( !this.getMethod().equals(HttpMethod.GET) ) {
	        	BufferedOutputStream b = new BufferedOutputStream(con.getOutputStream());
	        	b.write(body.getBytes("UTF-8"));
	        	b.flush();
			}

        	// Get response
        	@SuppressWarnings("unchecked")
			HttpResponse<Q> response = (HttpResponse<Q>) RestServer.readResponse(con, type);
        	con.getInputStream().close();
        	if ( response == null ) {
        		return new ResponseEntity<Q>(HttpStatus.NOT_FOUND);
        	} else {
        		return new ResponseEntity<Q>(response.getStatus(), response.getHeaders(), response.getBody());
        	}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new ResponseEntity<Q>(HttpStatus.BAD_REQUEST);
	}
}
