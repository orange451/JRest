package jrest;

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

	public EndPoint<Q,P> getEndpoint() {
		return this.endpoint;
	}
	
	public MediaType getConsumes() {
		return this.consumes;
	}
	
	public MediaType getProduces() {
		return this.produces;
	}
	
	public Class<P> getBodyType() {
		return this.bodyType;
	}

	public ResponseEntity<Q> query(HttpRequest<P> request) {
		try {
			//String bodyString = request.getBody()==null?new String():request.getBody().toString();
			HttpRequest<P> useRequest = new HttpRequest<P>(request.getMethod(), request.getHeaders(), request.getBody());
			useRequest.uri = request.getURI();
			useRequest.urlParams = request.getUrlParameters();
			useRequest.cookies = request.getCookies();
			return getEndpoint().run(useRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
