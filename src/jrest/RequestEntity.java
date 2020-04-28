package jrest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
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
	
	public void exchangeAsync(String url, Class<T> type, AsyncResponse<T> response) throws MalformedURLException {
		this.exchangeAsync(new URL(url), type, response);
	}
	
	public void exchangeAsync(URL url, Class<T> type, AsyncResponse<T> response) {
		new Thread(()->{
			try {
				response.response(exchange(url, type));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	}
	
	public ResponseEntity<T> exchange(String url, Class<T> type) throws MalformedURLException,IOException {
		return this.exchange(new URL(url), type);
	}
	
	public ResponseEntity<T> exchange(URL url, Class<T> type) throws IOException {
		// Connect to endpoint
		//Socket serverConnection = new Socket();
		try {
			// Find port
			/*int port = url.getDefaultPort();
			if ( port == -1 && url.getHost().startsWith("http://") )
				port = 80;
			if ( port == -1 && url.getHost().startsWith("https://") )
				port = 443;*/
			
			// Connect
			//serverConnection.connect(new InetSocketAddress(url.getHost(), 80), 10000);
			//serverConnection.setKeepAlive(true);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoInput(true);
			if ( this.getMethod().equals(HttpMethod.POST) )
				con.setDoOutput(true);
			con.setRequestMethod(this.getMethod().toString());
			
			for (Entry<String, String> entry : this.getHeaders().entrySet()) {
				try { con.setRequestProperty(entry.getKey(), entry.getValue()); } catch( Exception e) {}
			}
			
			// Hidden headers
			if ( this.getHeaders().get("Host") == null )
				this.getHeaders().put("Host", url.getHost());
			
			// Write initial header
			//b.write(new String( this.getMethod() + " " + url.getPath() + " HTTP/1.1\n").getBytes("UTF-8"));

        	// Write headers
            for (Entry<String, String> entry : this.getHeaders().entrySet()) {
            	//b.write(new String(entry.getKey() + ": " + entry.getValue() + "\n").getBytes("UTF-8"));
            }
            
            // Get usable body
        	String body = null;
        	if ( getBody() != null )
        		body = getBody().toString();
        	else
        		body = new String();
            
        	// Write body
        	//b.write(new String("Content-Length: "+body.toString().length()+"\n\n"+body).getBytes("UTF-8"));
			if ( !this.getMethod().equals(HttpMethod.GET) ) {
	        	BufferedOutputStream b = new BufferedOutputStream(con.getOutputStream());
	        	b.write(body.getBytes("UTF-8"));
	        	b.flush();
			}

        	// Get response
        	@SuppressWarnings("unchecked")
			HttpResponse<T> response = (HttpResponse<T>) RestServer.readResponse(con, type);
        	//b.close();
        	if ( response == null ) {
        		return new ResponseEntity<T>(HttpStatus.NOT_FOUND);
        	} else {
        		return new ResponseEntity<T>(response.getStatus(), response.getHeaders(), response.getBody());
        	}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new ResponseEntity<T>(HttpStatus.BAD_REQUEST);
	}
}
