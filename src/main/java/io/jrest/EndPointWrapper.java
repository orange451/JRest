package io.jrest;

class EndPointWrapper<P, Q> {
	private EndPoint<Q,P> endpoint;
	private MediaType consumes;
	private MediaType produces;
	private Class<P> bodyType;

	public EndPointWrapper(EndPoint<Q,P> endpoint, MediaType consumes, MediaType produces, Class<P> bodyType) {
		this.endpoint = endpoint;
		this.consumes = consumes;
		this.produces = produces;
		this.bodyType = bodyType;
	}

	/**
	 * Returns the endpoint object where business logic is defined.
	 */
	public EndPoint<Q,P> getEndpoint() {
		return this.endpoint;
	}
	
	/**
	 * Media Type that this endpoint desires to consume.
	 */
	public MediaType getConsumes() {
		return this.consumes;
	}
	
	/**
	 * Media Type that this endpoint should produce.
	 */
	public MediaType getProduces() {
		return this.produces;
	}
	
	/**
	 * Java Class that represents what data-type this endpoint produces. Similar to {@link #getProduces()}.
	 */
	public Class<P> getBodyType() {
		return this.bodyType;
	}

	/**
	 * Query the endpoint with a given request object.
	 */
	public ResponseEntity<Q> query(HttpRequest<P> request) {
		try {
			HttpRequest<P> useRequest = new HttpRequest<P>(request.getMethod(), request.getHeaders(), request.getBody());
			useRequest.uri = request.getURI();
			useRequest.urlParams = request.getUrlParameters();
			useRequest.cookies = request.getCookies();
			
			if ( request.hasSession() )
				useRequest.setSession(request.session());
			
			ResponseEntity<Q> response = getEndpoint().run(useRequest);
			
			if ( useRequest.hasSession() )
				request.setSession(useRequest.session());
			
			return response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
