package io.jrest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLSocketFactory;

import com.sun.net.ssl.HostnameVerifier;
import com.sun.net.ssl.HttpsURLConnection;

@SuppressWarnings({ "deprecation", "restriction" })
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
		this.cookies = new ArrayList<HttpCookie>(JRest.cookieManager.getCookieStore().getCookies());
	}
	
	/**
	 * HTTP Method used to invoke a HTTP Request
	 */
	public HttpMethod getMethod() {
		return this.method;
	}
	
	/**
	 * Queries a specified endpoint asynchronously.
	 * @throws MalformedURLException
	 */
	@SuppressWarnings("unchecked")
	public <P, Q> void exchangeAsync(String url, AsyncResponse<Q> response) throws MalformedURLException {
		this.exchangeAsync(url, (Class<Q>)Object.class, response);
	}

	
	/**
	 * Queries a specified endpoint asynchronously.
	 * @throws MalformedURLException
	 */
	public <P, Q> void exchangeAsync(String url, Class<Q> responseType, AsyncResponse<Q> response) throws MalformedURLException {
		this.exchangeAsync(new URL(url), responseType, response);
	}

	
	/**
	 * Queries a specified endpoint asynchronously.
	 */
	@SuppressWarnings("unchecked")
	public <P, Q> void exchangeAsync(URL url, AsyncResponse<Q> response) {
		this.exchangeAsync(url, (Class<Q>)Object.class, response);
	}

	
	/**
	 * Queries a specified endpoint asynchronously.
	 */
	public <P, Q> void exchangeAsync(URL url, Class<Q> responseType, AsyncResponse<Q> response) {
		new Thread(()->{
			try {
				response.response(exchange(url, responseType));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	/**
	 * Queries a specified endpoint. Returns a response entity object describing the result.
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public <Q> ResponseEntity<Q> exchange(String url, Class<Q> responseType) throws MalformedURLException,IOException {
		return this.exchange(new URL(url), responseType);
	}

	/**
	 * Queries a specified endpoint. Returns a response entity object describing the result.
	 * @throws IOException
	 */
	public <P, Q> ResponseEntity<Q> exchange(URL url, Class<Q> responseType) throws IOException {
		// Connect to endpoint
		try {
			
			// Manual check for url form encoded
			boolean bodyInUrl = false;
			String urlParameters = null;
			if ( this.getHeaders() != null && this.getHeaders().getContentType().equals(MediaType.APPLICATION_FORM_URLENCODED_VALUE) && this.getBody() instanceof Map ) {
				bodyInUrl = true;
				urlParameters = "";
			    for (Object key : ((Map<?, ?>)this.getBody()).keySet())
			    	urlParameters = urlParameters + key + "=" + ((Map<?, ?>)this.getBody()).get(key) + "&";
			    urlParameters.substring(0, urlParameters.length()-1);
			}
			
			// Connect
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoInput(true);
			if ( this.getMethod().equals(HttpMethod.POST) )
				con.setDoOutput(true);
			con.setRequestMethod(this.getMethod().toString());
			
			// Deprecated HttpsURLConnection stuff
			if ( con instanceof HttpsURLConnection ) {
				HttpsURLConnection httpsCon = (HttpsURLConnection)con;
				httpsCon.setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String urlHostname, String certHostname) {
						return HttpsURLConnection.getDefaultHostnameVerifier().verify(urlHostname, certHostname);
					}
		        });
				httpsCon.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
			}

			// Hidden headers
			if ( this.getHeaders().get(HttpHeaders.HOST) == null ) {
				String port = url.getPort() == -1 ? "" : (":" + url.getPort());
				this.getHeaders().put(HttpHeaders.HOST, url.getHost() + port);
			}
			
			// User agent???
			if ( this.getHeaders().get(HttpHeaders.USER_AGENT) == null ) {
				con.setRequestProperty(HttpHeaders.USER_AGENT, "Mozilla/5.0 (" + System.getProperty("os.name") + ") Java/" + System.getProperty("java.version"));
			}
			
			// Cookies!
			if (getCookies().size() > 0) {
				List<HttpCookie> cookiesList = getCookies();
				List<String> cookies = new ArrayList<>();
				for (HttpCookie cookie : cookiesList) {
					cookie.setVersion(0); // Sending cookies use simple version!
					cookies.add(cookie.toString());
				}
				con.setRequestProperty("Cookie", String.join(";", cookies));
			}
			
        	// Write headers
			for (Entry<String, String> entry : this.getHeaders().entrySet()) {
				try { con.setRequestProperty(entry.getKey(), entry.getValue()); } catch( Exception e) {}
			}
            
            // Get usable body
        	String body = null;
        	if ( getBody() != null && !bodyInUrl ) {
        		body = RestUtil.convertToString(getBody());
        	} else {
        		body = new String();
        	}
        	
        	if ( bodyInUrl )
        		body = urlParameters;
            
        	// Write body
			if ( !this.getMethod().equals(HttpMethod.GET) ) {
	        	BufferedOutputStream b = new BufferedOutputStream(con.getOutputStream());
	        	b.write(StringUtil.utf8(body));
	        	b.flush();
			}

        	// Get response
        	@SuppressWarnings("unchecked")
			HttpResponse<Q> response = (HttpResponse<Q>) RestUtil.readResponse(con, responseType);
        	con.getInputStream().close();
        	con.disconnect();
        	if ( response == null ) {
        		return new ResponseEntity<Q>(HttpStatus.NOT_FOUND);
        	} else {
        		ResponseEntity<Q> res = new ResponseEntity<Q>(response.getStatus(), response.getHeaders(), response.getBody());
        		res.cookies = response.cookies;
        		for (HttpCookie cookie : res.cookies) {
        			try {
						JRest.cookieManager.getCookieStore().add(url.toURI(), cookie);
					} catch (URISyntaxException e) {
						//
					}
        		}
        		return res;
        	}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new ResponseEntity<Q>(HttpStatus.BAD_REQUEST);
	}
}
