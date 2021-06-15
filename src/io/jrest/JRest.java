package io.jrest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.jrest.Logger.LogType;

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
	
	/** Client use of cookies **/
	protected static CookieManager cookieManager;
	
	/** Server use of sessions **/
	protected static SessionStorage sessionStorage;
	
	/** Server use of cookies **/
	private static Map<JRest, Map<Socket, CookieManager>> cookieManagerServer;
	
	/** Logger used for output **/
	private Logger logger;
	
	static {
		cookieManagerServer = new HashMap<>();
		cookieManager = new CookieManager();
		sessionStorage = new SessionStorage();
	}
	
	/** Use {@link JRest#create()} to create a new JRest instance **/
	private JRest() {
		this.port = 80;
		this.logger = new Logger();
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
			this.getLogger().warn("Server cannot be stopped as it has not yet been started.");
			return this;
		}
		
		// Server must exist
		if ( server == null ) {
			this.getLogger().warn("Server is still starting... Sending flag to shutdown.");
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
			this.getLogger().error("Server is already started on port: " + port);
			return this;
		}
		
		// Server starting
		if ( started ) {
			this.getLogger().warn("Server is currently initializing. Please wait");
			return this;
		}
		
		// Setup cookie handler
		cookieManager = new CookieManager();
		started = true;
		initializing = true;
		
		// Start new server
		new JRestServer().start();

		// Wait for server to turn on
		while (!error && initializing) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				this.getLogger().error(e);
			}
		}
		
		return this;
	}

	private EndPointWrapper<?, ?> getEndPoint(String endpoint, HttpMethod method) {
		Map<HttpMethod, EndPointWrapper<?,?>> map = endpointMap.get(endpoint);
		if (map == null)
			return null;

		return map.get(method);
	}
	
	class JRestServer extends Thread implements Runnable {
		private final JRest jrestInstance = JRest.this;
		private final long startTime = System.currentTimeMillis();
		
		public void run() {
			try {
				server = new ServerSocket(port);
				server.setSoTimeout(0);
				
				long elaspedTime = System.currentTimeMillis()-startTime;
				jrestInstance.getLogger().trace("JREST Server started: " + Inet4Address.getLocalHost().getHostAddress() + ":" + server.getLocalPort() + " " + elaspedTime + " ms");
				
				ExecutorService service = Executors.newCachedThreadPool();
				initializing = false;
				
				while (started) {
					try {
						// Dont burn CPU while waiting for connections
						Thread.yield();
						
						// Wait for socket
						Socket incoming = server.accept();

						// Start listening to its data
						if (incoming != null)
							service.submit(()->readAndHandleSocket(incoming));
					} catch (SocketTimeoutException e) {
						jrestInstance.getLogger().error(e);
					} catch (IOException e) {
						jrestInstance.getLogger().error(e);
					}
				}
				
				jrestInstance.getLogger().trace("Shutting down " + jrestInstance.getServerName());
				service.shutdown();
			} catch (IOException e1) {
				jrestInstance.getLogger().error("Error making server... ", e1);
				error = true;
				started = false;
				initializing = false;
			} finally {
				try {
					server.close();
				} catch (IOException e) {
					jrestInstance.getLogger().error("Error stopping server... ", e);
				}
				started = false;
				initializing = false;
				server = null;
			}
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
					handleRequest(incoming, request);

					// Close socket when done
					incoming.close();
				}
			} catch (Exception e) {
				jrestInstance.getLogger().error(e);
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
			List<String> headerData = RestUtil.readRequestData(inputStream);
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
			Map<String, String> urlparams = new HashMap<>();
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
			CookieManager cookieManager = cookieManagerServer.get(jrestInstance).get(incoming);
			if (cookieManager == null)
				cookieManagerServer.get(jrestInstance).put(incoming, cookieManager = new CookieManager());
			
			// Read in cookies
			HttpSession session = null;
			String cookiesHeader = headers.get("Cookie");
			if (cookiesHeader != null) {
				String[] cookiesSplit = cookiesHeader.split(";"); // TODO find a better way to split the cookies. This format is not guaranteed.
				for (String cookieString : cookiesSplit) {
					List<HttpCookie> cookies = HttpCookie.parse(cookieString);
					for (HttpCookie hcookie : cookies) {
						if ( hcookie.getName().equalsIgnoreCase(HttpSession.SESSION_NAME) ) {
							session = sessionStorage.get(hcookie.getValue());
						} else {
							cookieManager.getCookieStore().add(null, hcookie);
						}
					}
				}
			}

			// Get Body
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
			
			// Create request object
			HttpRequest<Object> request = new HttpRequest<>(method, headers, body);
			request.uri = uri;
			request.urlParams = urlparams;
			if (cookieManagerServer.get(jrestInstance).get(incoming) != null)
				request.cookies = new ArrayList<>(cookieManagerServer.get(jrestInstance).get(incoming).getCookieStore().getCookies());
			else 
				request.cookies = new ArrayList<>();
			
			request.setSession(session);

			// Return
			return request;
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

		/**
		 * Runs when client makes http request to one of our endpoints.
		 */
		@SuppressWarnings("unchecked")
		private <P,Q> void handleRequest(Socket socket, HttpRequest<P> request) throws UnsupportedEncodingException, IOException {
			// Log
			if (request != null)
				jrestInstance.getLogger().trace("[" + new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()) + "] Incoming request: " + request);

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
					cookieManagerServer.get(jrestInstance).get(socket).getCookieStore().add(null, cookie);
				}

				Object body = response.getBody();
				if (body == null)
					body = new String();

				// Convert body
				String writeBody = RestUtil.convertSoString(body);

				// Get Cookie List
				List<HttpCookie> cookiesList = new ArrayList<>();
				if (cookieManagerServer.get(jrestInstance).containsKey(socket))
					for (HttpCookie cookie : cookieManagerServer.get(jrestInstance).get(socket).getCookieStore().getCookies())
						cookiesList.add(cookie);
				
				// Add in session (only if it exists, we dont want to generate one)
				if ( request.hasSession() && request.session().isValid() )
					cookiesList.add(request.session().toCookie());
				
				// Write response
				RestUtil.write(socket, jrestInstance.getServerName(), status, produces, writeBody, response.getHeaders(), cookiesList);
				socket.getOutputStream().close();
			}
		}
	}
	
	static class SessionStorage {
		private Map<String, HttpSession> storage = new HashMap<>();
		
		/**
		 * Get a session by its uuid string.
		 */
		public HttpSession get(String uuid) {
			return storage.get(uuid);
		}
		
		/**
		 * Create a new HttpSession and add it to the storage.
		 */
		public HttpSession create() {
			HttpSession session = new HttpSession();
			storage.put(session.getUUID().toString(), session);
			return session;
		}
		
		/**
		 * Returns all active sessions. See {@link HttpSession#isValid()}.
		 */
		public List<HttpSession> getSessions() {
			List<HttpSession> sessions = new ArrayList<>();
			
			for (HttpSession session : storage.values()) {
				if ( !session.isValid() )
					continue;
				
				sessions.add(session);
			}
			
			return sessions;
		}
		
		/**
		 * Loads a list of sessions in to session storage.
		 */
		public void loadSessions(List<HttpSession> sessions) {
			for (HttpSession session : sessions) {
				storage.put(session.getUUID().toString(), session);
			}
		}
	}
	
	/**
	 * Get the logger object used to log data.
	 */
	public Logger getLogger() {
		return this.logger;
	}
	
	/**
	 * Set the internal log level used for logging data.
	 * Same as calling {@link Logger#setLogType(LogType)} from {@link JRest#getLogger()}.
	 */
    public JRest setLogType(LogType type) {
    	getLogger().setLogType(type);
    	return this;
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
			this.getLogger().error("Port cannot be specified on a server that is starting or has been started.");
			return this;
		}
		
		this.port = port;
		return this;
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
	public <P, Q> JRest setResponseHandler(HttpStatus status, MediaType produces, Class<P> bodyType, EndPoint<Q,P> endpointObject) {
		if ( this.isErrored() ) {
			this.getLogger().error("Could not register response handler. Server failed to start.");
			return this;
		}
		responseHandlerMap.put(status, new EndPointWrapper<P, Q>(endpointObject, produces, produces, bodyType));
		this.getLogger().debug("Registered Response Handler\t[" + status + "]");
		return this;
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
	public <P, Q> JRest setResponseHandler(HttpStatus status, MediaType produces, EndPoint endpointObject) {
		return setResponseHandler(status, produces, Object.class, endpointObject);
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
	public <P, Q> JRest setResponseHandler(HttpStatus status, EndPoint endpointObject) {
		return setResponseHandler(status, MediaType.TEXT_PLAIN, Object.class, endpointObject);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param consumes Type of media this endpoint will consume
	 * @param produces Type of media this endpoint will produce
	 * @param bodyType Type of class we expect to receive with our response
	 * @param returnType Type of class we expect to send with our response
	 * @param object   Business logic interface
	 */
	public <P, Q> JRest addEndpoint(HttpMethod method, String endpoint, MediaType consumes, MediaType produces, Class<P> bodyType, Class<Q> returnType, EndPoint<Q,P> object) {
		if ( this.isErrored() ) {
			this.getLogger().error("Could not register endpoint. Server failed to start.");
			return this;
		}
		if ( !this.isStarted() ) {
			this.getLogger().error("Could not register endpoint. Server is not started.");
			return this;
		}
		if (!endpointMap.containsKey(endpoint)) {
			endpointMap.put(endpoint, new HashMap<>());
		}

		Map<HttpMethod, EndPointWrapper<?,?>> t = endpointMap.get(endpoint);
		if (t == null)
			return this;

		t.put(method, new EndPointWrapper<P, Q>(object, consumes, produces, bodyType));
		this.getLogger().debug("Registered endpoint\t[" + method + "]\t " + endpoint);
		return this;
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * @param <T>
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param consumes Type of media this endpoint will consume
	 * @param produces Type of media this endpoint will produce
	 * @param bodyType Type of class we expect to receive/send with our response
	 * @param object   Business logic interface
	 */
	@SuppressWarnings("unchecked")
	public <P, Q> JRest addEndpoint(HttpMethod method, String endpoint, MediaType consumes, MediaType produces, Class<P> bodyType, EndPoint<Q,P> object) {
		Class<Q> returnType = (Class<Q>) bodyType;
		JRest ret = this.addEndpoint(method, endpoint, consumes, produces, bodyType, returnType, object);
		return ret;
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
	public <P, Q> JRest addEndpoint(HttpMethod method, String endpoint, MediaType produceAndConsume, Class<P> bodyType, EndPoint<Q,P> object) {
		return addEndpoint(method, endpoint, produceAndConsume, produceAndConsume, bodyType, object);
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
	public <P, Q> JRest addEndpoint(HttpMethod method, String endpoint, Class<P> bodyType, EndPoint<Q,P> object) {
		return addEndpoint(method, endpoint, MediaType.TEXT_PLAIN, bodyType, object);
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
	public <P,Q> JRest addEndpoint(HttpMethod method, String endpoint, MediaType consumes, MediaType produces, EndPoint object) {
		return addEndpoint(method, endpoint, consumes, produces, Object.class, object);
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
	public <P,Q> JRest addEndpoint(HttpMethod method, String endpoint, MediaType produceAndConsume, EndPoint<Q,P> object) {
		return addEndpoint(method, endpoint, produceAndConsume, produceAndConsume, object);
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
	public <P,Q> JRest addEndpoint(HttpMethod method, String endpoint, EndPoint<Q,P> object) {
		return addEndpoint(method, endpoint, MediaType.TEXT_PLAIN, object);
	}

	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses GET request.
	 * 
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> JRest addEndpoint(String endpoint, EndPoint<Q,P> object) {
		return addEndpoint(HttpMethod.GET, endpoint, object);
	}
	
	/**
	 * Registers a mixed-rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * <br>
	 * A mixed-rest endpoint is an endpoint that returns data in a different format than it was received.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> JRest addEndpoint(HttpMethod method, String endpoint, MediaType produceAndConsume, Class<P> requestType, Class<Q> returnType, EndPoint<Q,P> object) {
		return this.addEndpoint(method, endpoint, produceAndConsume, produceAndConsume, requestType, returnType, object);
	}
	
	/**
	 * Registers a mixed-rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. MediaType will be {@link MediaType#APPLICATION_JSON}
	 * unless ClassType is String, in which case it will be {@link MediaType#TEXT_PLAIN}
	 * <br>
	 * A mixed-rest endpoint is an endpoint that returns data in a different format than it was received.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> JRest addEndpoint(HttpMethod method, String endpoint, Class<P> requestType, Class<Q> returnType, EndPoint<Q,P> object) {
		MediaType reqType = requestType == String.class ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON;
		MediaType retType = returnType == String.class ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON;
		return this.addEndpoint(HttpMethod.GET, endpoint, reqType, retType, requestType, returnType, object);
	}
	
	/**
	 * Registers a mixed-rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses GET request.
	 * MediaType will be {@link MediaType#APPLICATION_JSON} unless ClassType is String,
	 * in which case it will be {@link MediaType#TEXT_PLAIN}
	 * <br>
	 * A mixed-rest endpoint is an endpoint that returns data in a different format than it was received.
	 * 
	 * @param method   HTTP Method required to communicate with this endpoint.
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> JRest addEndpoint(String endpoint, Class<P> requestType, Class<Q> returnType, EndPoint<Q,P> object) {
		return this.addEndpoint(HttpMethod.GET, endpoint, requestType, returnType, object);
	}
	
	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact.
	 * @param EndpointBuilder
	 * @return
	 */
	public <P, Q> JRest addEndpoint(EndpointBuilder<P, Q> builder) {
		if ( builder.getRequest() == null ) {
			this.getLogger().error("Could not register endpoint. Please Set Request Callback.");
			return this;
		}
		
		return this.addEndpoint(builder.getHttpMethod(), builder.getEndpoint(), builder.getConsumes(), builder.getProduces(), builder.getReceiveType(), builder.getReturnType(), builder.getRequest());
	}
	
	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses GET request.
	 * 
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> JRest get(String endpoint, EndPoint<Q,P> object) {
		return addEndpoint(HttpMethod.GET, endpoint, object);
	}
	
	/**
	 * Registers a rest endpoint to the rest server. This endpoint acts as an end of
	 * a communication channel from which APIs can interact. Uses POST request.
	 * 
	 * @param endpoint Endpoint API URL (Start with /)
	 * @param object   Business logic interface
	 */
	public <P,Q> JRest post(String endpoint, EndPoint<Q,P> object) {
		return addEndpoint(HttpMethod.POST, endpoint, object);
	}
}
