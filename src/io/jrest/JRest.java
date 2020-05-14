package io.jrest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JRest {
	
	/** Internal server socket used to send data to clients **/
	private static ServerSocket server;
	
	/** Internal map to quickly locate endpoints **/
	private final Map<String, Map<HttpMethod, EndPointWrapper<?,?>>> endpointMap;
	
	private final Map<HttpStatus, EndPointWrapper<?,?>> responseHandlerMap;

	/** Whether the server is started **/
	private boolean started;
	
	/** Whether the server encountered an error starting **/
	private boolean error;
	
	/** Whether the server is currently initializing **/
	private boolean initializing;
	
	/** Name of the server when a request is made **/
	private String serverName;
	
	/** Port the server is running on **/
	private int port;
	
	/** Whether requests get logged to console **/
	private boolean logRequests = true;
	
	/** Client use of cookies **/
	protected static CookieManager cookieManager;
	
	/** Server use of cookies **/
	private static Map<JRest, Map<Socket, CookieManager>> cookieManagerServer;
	
	static {
		cookieManagerServer = new HashMap<>();
		cookieManager = new CookieManager();
	}
	
	/** Use {@link JRest#create()} to create a new JRest instance **/
	private JRest() {
		this.port = 80;
		this.endpointMap = new HashMap<>();
		this.responseHandlerMap = new HashMap<>();
		this.serverName = "JRest : Lightweight REST Server";
		cookieManagerServer.put(this, new HashMap<>());
	}
	
	/** Create new JRest instance. **/
	public static JRest create() {
		return new JRest();
	}
	
	/** Stop the server */
	public JRest stop() {

		// Server not started
		if ( !started ) {
			System.err.println("Server cannot be stopped as it has not yet been started.");
			return this;
		}
		
		// Server must exist
		if ( server == null ) {
			System.err.println("Server is still starting... Sending flag to shutdown.");
			started = false;
			return this;
		}
		
		// Stop
		this.started = false;
		return this;
	}

	/** Start server **/
	public JRest start() {
		
		// Server initializing
		if ( server != null ) {
			System.err.println("Server is already started on port: " + port);
			return this;
		}
		
		// Server starting
		if ( started ) {
			System.err.println("Server is currently initializing. Please wait");
			return this;
		}
		
		// Setup cookie handler
		cookieManager = new CookieManager();
		started = true;
		initializing = true;
		
		long startTime = System.currentTimeMillis();
		
		// Start new server
		new Thread(() -> {
			try {
				server = new ServerSocket(port);
				server.setSoTimeout(0);
				
				long elaspedTime = System.currentTimeMillis()-startTime;
				System.out.println("REST Server started: " + Inet4Address.getLocalHost().getHostAddress() + ":" + server.getLocalPort() + " " + elaspedTime + " ms");
				
				ExecutorService service = Executors.newCachedThreadPool();
				initializing = false;
				
				while (started) {
					try {
						// Dont burn CPU while waiting for connections
						Thread.sleep(5);
						Thread.yield();
						
						// Wait for socket
						Socket incoming = server.accept();

						// Start listening to its data
						if (incoming != null)
							service.submit(()->readAndHandleSocket(incoming));
					} catch (SocketTimeoutException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				System.out.println("Shutting down " + this.getServerName());
				service.shutdown();
			} catch (IOException e1) {
				System.err.println("Error making server... " + e1);
				e1.printStackTrace();
				error = true;
				started = false;
				initializing = false;
			} finally {
				try {
					server.close();
				} catch (IOException e) {
					System.err.println("Error stopping server... " + e);
					e.printStackTrace();
				}
				started = false;
				initializing = false;
				server = null;
			}
		}).start();

		// Wait for server to turn on
		while (!error && initializing) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return this;
	}

	/**
	 * Reads and handles an incoming socket connection
	 */
	private void readAndHandleSocket(Socket incoming) {
		try {
			while (!incoming.isClosed()) {
				// Parse sockets request
				HttpRequest<?> request = parseRequest(incoming);
				if (request == null) {
					Thread.sleep(1); // Dont burn CPU
					continue;
				}

				// Run REST endpoint logic
				onRequest(incoming, request);

				// Close socket when done
				incoming.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets HttpRequest from socket connection
	 */
	private <T> HttpRequest<Object> parseRequest(Socket incoming) throws IOException {
		// Get incoming info
		String address = incoming.getInetAddress().getHostAddress();
		int port = incoming.getPort();
		InputStream inputStream = incoming.getInputStream();
		
		// Parse input into strings
		List<String> headerData = readRequestData(inputStream);
		if (headerData == null || headerData.size() == 0)
			return null;
		Object body = headerData.remove(headerData.size() - 1);

		// Must have 2 strings
		if (headerData.size() < 2)
			return null;

		// Get some header info
		String[] t1 = headerData.get(0).split(" ");
		HttpMethod method = HttpMethod.valueOf(t1[0]);
		String apiString = t1[1];
		String[] apisplit = apiString.split("\\?", 2);
		String api = apisplit[0];
		Map<String, String> urlparams = null;
		if ( apisplit.length > 1 )
			urlparams = convertParams(apisplit[1]);

		// Create headers
		HttpHeaders headers = new HttpHeaders();
		for (String string : headerData) {
			String[] split = string.split(":", 2);
			if (split.length != 2)
				continue;
			String key = split[0].trim();
			String value = split[1].trim();

			headers.put(key, value);
		}
		
		// Setup cookies
		CookieManager cookieManager = cookieManagerServer.get(this).get(incoming);
		if (cookieManager == null)
			cookieManagerServer.get(this).put(incoming, cookieManager = new CookieManager());
		
		// Read in cookies
		String cookiesHeader = headers.get("Cookie");
		if (cookiesHeader != null) {
			String[] cookies = cookiesHeader.split(";");
			for (String cookie : cookies) {
				cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
			}
		}

		// Create request object
		String host = address.replace("0:0:0:0:0:0:0:1", "127.0.0.1");
		URI uri = URI.create("http://" + host + ":" + port + api);
		EndPointWrapper<?, ?> endpoint = getEndPoint(uri.getPath(), method);
		if (endpoint != null) {
			if ( endpoint.getConsumes().equals(MediaType.APPLICATION_FORM_URLENCODED) ) {
				urlparams = convertParams(body.toString());
				body = null;
			} else {
				body = RestUtil.convertObject(body.toString(), endpoint.getBodyType());
			}
		}
		HttpRequest<Object> request = new HttpRequest<>(method, headers, body);
		request.uri = uri;
		request.urlParams = urlparams;
		if (cookieManagerServer.get(this).get(incoming) != null)
			request.cookies = new ArrayList<>(cookieManagerServer.get(this).get(incoming).getCookieStore().getCookies());
		else 
			request.cookies = new ArrayList<>();

		// Return
		return request;
	}

	/**
	 * Runs when client makes http request to one of our endpoints.
	 */
	@SuppressWarnings("unchecked")
	private <P,Q> void onRequest(Socket socket, HttpRequest<P> request) throws UnsupportedEncodingException, IOException {
		// Log
		if (request != null && isLogRequests())
			System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()) + "] Incoming request: " + request);

		// Get matching endpoint
		EndPointWrapper<P, Q> endpoint = (EndPointWrapper<P, Q>) getEndPoint(request.getURI().getPath(), request.getMethod());
		MediaType produces = MediaType.TEXT_PLAIN;
		HttpStatus status = HttpStatus.NOT_FOUND;
		ResponseEntity<Q> response = null;
		
		// Query endpoint
		if (endpoint != null) {
			produces = endpoint.getProduces();
			response = endpoint.query(request);
			if (response == null)
				response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			
			status = response.getStatus();
		}
		
		// Status Handler override
		if ( responseHandlerMap.containsKey(status) ) {
			endpoint = (EndPointWrapper<P, Q>) responseHandlerMap.get(status);
			if ( endpoint != null ) {
				produces = endpoint.getProduces();
				response = endpoint.query(request);
				if (response == null)
					response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
				
				status = response.getStatus();
			}
		}
		
		if ( response != null ) {
			// Put response cookies into manager
			for (HttpCookie cookie : response.getCookies()) {
				cookieManagerServer.get(this).get(socket).getCookieStore().add(null, cookie);
			}

			Object body = response.getBody();
			if (body == null)
				body = new String();

			// Convert body
			String writeBody = RestUtil.convertSoString(body);

			// Write response
			BufferedOutputStream b = new BufferedOutputStream(socket.getOutputStream());
			b.write(new String("HTTP/1.1 " + response.getStatus().value() + " " + response.getStatus().getReasonPhrase() + "\n").getBytes("UTF-8"));
			b.write(new String("Keep-Alive: " + "timeout=5, max=99" + "\n").getBytes("UTF-8"));
			b.write(new String("Server: " + this.getServerName() + "\n").getBytes("UTF-8"));
			b.write(new String("Connection: " + "Keep-Alive" + "\n").getBytes("UTF-8"));
			
			// Write cookies to user
			if (cookieManagerServer.get(this).containsKey(socket) && cookieManagerServer.get(this).get(socket).getCookieStore().getCookies().size() > 0) {
				List<HttpCookie> cookiesList = cookieManagerServer.get(this).get(socket).getCookieStore().getCookies();
				List<String> cookies = new ArrayList<>();
				for (HttpCookie cookie : cookiesList)
					cookies.add(cookie.toString());
				b.write(new String("Cookie: " + String.join(";", cookies) + "\n").getBytes("UTF-8"));
			}
			
			b.write(new String("Content-Length: " + writeBody.length() + "\n").getBytes("UTF-8"));
			b.write(new String("Content-Type: " + produces + "\n\n").getBytes("UTF-8"));
			b.write(new String(writeBody).getBytes("UTF-8"));
			b.flush();
			b.close();
		}
	}
	
	/**
	 * Convert standard URL Parameters to map
	 */
	private Map<String, String> convertParams(String str) {
		Map<String, String> params = new HashMap<>();
		String[] paramsplit = str.split("&");
		for (String paramStr : paramsplit) {
			String[] t = paramStr.split("=", 2);
			if (t.length == 2) {
				params.put(t[0], t[1]);
			}
		}
		
		return params;
	}

	protected static <T> HttpResponse<T> readResponse(HttpURLConnection connection, T type) throws IOException {
		// Read body
		byte[] data = RestUtil.readAll(connection.getInputStream());
		String body = new String(data, Charset.forName("UTF-8"));

		// Create response headers
		HttpHeaders headers = new HttpHeaders();
		Map<String, List<String>> map = connection.getHeaderFields();
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			headers.put(entry.getKey(), entry.getValue().get(0));
		}
		
		// Update cookies
		List<String> cookiesHeader = map.get("Cookie");
		if (cookiesHeader != null) {
		    for (String cookie : cookiesHeader) {
		        cookieManager.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
		    }               
		}

		// Create response object
		T tBody = RestUtil.convertObject(body, type);
		HttpResponse<T> request = new HttpResponse<>(HttpStatus.valueOf(connection.getResponseCode()), headers, tBody);
		request.cookies = new ArrayList<>(cookieManager.getCookieStore().getCookies());

		// Return
		return request;
	}

	private static List<String> readRequestData(InputStream inputStream) throws IOException {
		long TIMEOUT = System.currentTimeMillis() + 1000;

		BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
		while (bufferedInput.available() == 0) {
			if (System.currentTimeMillis() > TIMEOUT) {
				bufferedInput.close();
				return Arrays.asList();
			}
		}

		// Parse input into strings
		List<String> headerData = new ArrayList<String>();
		boolean buildingHeader = true;
		StringBuilder builder = new StringBuilder();
		while (bufferedInput.available() > 0) {
			int c = bufferedInput.read();
			char ch = (char) (c);
			if (ch == '\r')
				continue;

			if (ch == '\n' && buildingHeader) {
				if (builder.length() == 0)
					buildingHeader = false;

				if (buildingHeader) {
					headerData.add(builder.toString().trim());
					builder.setLength(0);
				}
			} else {
				builder.append(ch);
			}
		}

		// BODY IS LAST
		headerData.add(builder.toString());
		return headerData;
	}

	private EndPointWrapper<?, ?> getEndPoint(String endpoint, HttpMethod method) {
		Map<HttpMethod, EndPointWrapper<?,?>> map = endpointMap.get(endpoint);
		if (map == null)
			return null;

		return map.get(method);
	}

	/**
	 * Returns whether the rest server has finished initializing.
	 */
	public boolean isStarted() {
		return this.started;
	}

	/**
	 * Returns whether the rest server encountered an error preventing it from
	 * running.
	 */
	public boolean isErrored() {
		return this.error;
	}

	/**
	 * Gets the name of the server used in HTTP responses
	 */
	public JRest setServerName(String name) {
		this.serverName = name;
		return this;
	}
	
	/**
	 * Sets the name of the server used in HTTP responses
	 */
	public String getServerName() {
		return this.serverName;
	}
	
	/**
	 * Return the port the server is running on.
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Set the port the server will run on. Must be called before starting the server.
	 */
	public JRest setPort(int port) {
		if ( started || server != null ) {
			System.err.println("Port cannot be specified on a server that is starting or has been started.");
			return this;
		}
		
		this.port = port;
		return this;
	}
	
	/**
	 * Sets whether requests are logged or not.
	 */
	public JRest setLogRequests(boolean log) {
		this.logRequests = log;
		return this;
	}
	
	/**
	 * Returns whether requests are logged or not.
	 */
	public boolean isLogRequests() {
		return this.logRequests;
	}

	/**
	 * Registers a rest response handler to the rest server. This response endpoint serves to
	 * inject custom responses for a given HttpStatus. For example, adding a response handler
	 * with status 404 allows for a custom 404 Not Found Page.
	 * 
	 * @param status   HTTP Status for the handler
	 * @param produces Type of media this endpoint will produce
	 * @param bodyType Type of class we expect to send with our response
	 * @param endpointObject   Business logic interface
	 */
	public <P, Q> void setResponseHandler(HttpStatus status, MediaType produces, Class<P> bodyType, EndPoint<Q,P> endpointObject) {
		if ( this.isErrored() ) {
			System.err.println("Could not register response handler. Server failed to start.");
			return;
		}
		responseHandlerMap.put(status, new EndPointWrapper<P, Q>(endpointObject, produces, produces, bodyType));
		System.out.println("Registered Response Handler\t[" + status + "]");
	}

	/**
	 * Registers a rest response handler to the rest server. This response endpoint serves to
	 * inject custom responses for a given HttpStatus. For example, adding a response handler
	 * with status 404 allows for a custom 404 Not Found Page.
	 * 
	 * @param status   HTTP Status for the handler
	 * @param produces Type of media this endpoint will produce
	 * @param endpointObject   Business logic interface
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <P, Q> void setResponseHandler(HttpStatus status, MediaType produces, EndPoint endpointObject) {
		setResponseHandler(status, produces, Object.class, endpointObject);
	}

	/**
	 * Registers a rest response handler to the rest server. This response endpoint serves to
	 * inject custom responses for a given HttpStatus. For example, adding a response handler
	 * with status 404 allows for a custom 404 Not Found Page.
	 * 
	 * @param status   HTTP Status for the handler
	 * @param endpointObject   Business logic interface
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <P, Q> void setResponseHandler(HttpStatus status, EndPoint endpointObject) {
		setResponseHandler(status, MediaType.TEXT_PLAIN, Object.class, endpointObject);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param consumes Type of media this endpoint will consume
	 * @param produces Type of media this endpoint will produce
	 * @param bodyType Type of class we expect to send with our response
	 * @param object   Business logic interface
	 */
	public <P, Q> void addEndpoint(HttpMethod method, String endpoint, MediaType consumes, MediaType produces, Class<P> bodyType, EndPoint<Q,P> object) {
		if ( this.isErrored() ) {
			System.err.println("Could not register endpoint. Server failed to start.");
			return;
		}
		if (!endpointMap.containsKey(endpoint)) {
			endpointMap.put(endpoint, new HashMap<>());
		}

		Map<HttpMethod, EndPointWrapper<?,?>> t = endpointMap.get(endpoint);
		if (t == null)
			return;

		t.put(method, new EndPointWrapper<P, Q>(object, consumes, produces, bodyType));
		System.out.println("Registered endpoint\t[" + method + "]\t " + endpoint);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param consumes Type of media this endpoint will consume and produce
	 * @param bodyType Type of class we expect to send with our response
	 * @param object   Business logic interface
	 */
	public <P, Q> void addEndpoint(HttpMethod method, String endpoint, MediaType produceAndConsume, Class<P> bodyType, EndPoint<Q,P> object) {
		addEndpoint(method, endpoint, produceAndConsume, produceAndConsume, bodyType, object);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses TEXT_PLAIN media
	 * type for produce/consume.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param bodyType Type of class we expect to send with our response
	 * @param object   Business logic interface
	 */
	public <P, Q> void addEndpoint(HttpMethod method, String endpoint, Class<P> bodyType, EndPoint<Q,P> object) {
		addEndpoint(method, endpoint, MediaType.TEXT_PLAIN, bodyType, object);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param consumes Type of media this endpoint will consume
	 * @param produces Type of media this endpoint will produce
	 * @param object   Business logic interface
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <P,Q> void addEndpoint(HttpMethod method, String endpoint, MediaType consumes, MediaType produces, EndPoint object) {
		addEndpoint(method, endpoint, consumes, produces, Object.class, object);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param consumes Type of media this endpoint will consume and produce
	 * @param object   Business logic interface
	 */
	public <P,Q> void addEndpoint(HttpMethod method, String endpoint, MediaType produceAndConsume, EndPoint<Q,P> object) {
		addEndpoint(method, endpoint, produceAndConsume, produceAndConsume, object);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses TEXT_PLAIN media
	 * type for produce/consume.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> void addEndpoint(HttpMethod method, String endpoint, EndPoint<Q,P> object) {
		addEndpoint(method, endpoint, MediaType.TEXT_PLAIN, object);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses GET request.
	 * 
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> void addEndpoint(String endpoint, EndPoint<Q,P> object) {
		addEndpoint(HttpMethod.GET, endpoint, object);
	}
}
