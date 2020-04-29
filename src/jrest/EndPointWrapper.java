package jrest;

class EndPointWrapper<T> {
	private EndPoint<T> endpoint;
	private MediaType consumes;
	private MediaType produces;
	private T bodyType;

	public EndPointWrapper(EndPoint<T> endpoint, MediaType consumes, MediaType produces, T bodyType) {
		this.endpoint = endpoint;
		this.consumes = consumes;
		this.produces = produces;
		this.bodyType = bodyType;
	}

	public EndPoint<T> getEndpoint() {
		return this.endpoint;
	}
	
	public MediaType getConsumes() {
		return this.consumes;
	}
	
	public MediaType getProduces() {
		return this.produces;
	}
	
	public T getBodyType() {
		return this.bodyType;
	}

	public ResponseEntity<T> query(HttpRequest<T> request) {
		try {
			String bodyString = request.getBody()==null?new String():request.getBody().toString();
			HttpRequest<T> useRequest = new HttpRequest<T>(request.getMethod(), request.getHeaders(), RestServer.getGenericObject(bodyString, getBodyType()));
			useRequest.uri = request.getURI();
			useRequest.urlParams = request.getUrlParameters();
			return getEndpoint().run(useRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
