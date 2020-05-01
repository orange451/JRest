package jrest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
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
	
	public <P, Q> void exchangeAsync(String url, Class<Q> responseType, AsyncResponse<Q> response) throws MalformedURLException {
		this.exchangeAsync(new URL(url), responseType, response);
	}
	
	public <P, Q> void exchangeAsync(URL url, Class<Q> responseType, AsyncResponse<Q> response) {
		new Thread(()->{
			try {
				response.response(exchange(url, responseType));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public ResponseEntity<T> exchange(String url, Class<T> responseType) throws MalformedURLException,IOException {
		return this.exchange(new URL(url), responseType);
	}
	
	public <P, Q> ResponseEntity<Q> exchange(URL url, Class<Q> responseType) throws IOException {
		// Connect to endpoint
		try {
			
			// Manual check for url form encoded
			boolean bodyInUrl = false;
			String urlParameters = null;
			if ( this.getHeaders() != null && this.getHeaders().getContentType().equals(MediaType.APPLICATION_FORM_URLENCODED_VALUE) && this.getBody() instanceof Map ) {
				bodyInUrl = true;
				urlParameters = "";
			    for (Object key : ((Map)this.getBody()).keySet())
			    	urlParameters = urlParameters + key + "=" + ((Map)this.getBody()).get(key) + "&";
			    urlParameters.substring(0, urlParameters.length()-1);
			}
			
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
        	if ( getBody() != null && !bodyInUrl )
        		body = getBody().toString();
        	else
        		body = new String();
        	
        	if ( bodyInUrl )
        		body = urlParameters;
            
        	// Write body
			if ( !this.getMethod().equals(HttpMethod.GET) ) {
	        	BufferedOutputStream b = new BufferedOutputStream(con.getOutputStream());
	        	b.write(body.getBytes("UTF-8"));
	        	b.flush();
			}

        	// Get response
        	@SuppressWarnings("unchecked")
			HttpResponse<Q> response = (HttpResponse<Q>) RestServer.readResponse(con, responseType);
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
