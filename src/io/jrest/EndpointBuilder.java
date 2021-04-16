package io.jrest;

public class EndpointBuilder<P,Q> {
	private String endpoint;
	private HttpMethod httpMethod;
	private MediaType consumes;
	private MediaType produces;
	private Class<P> receiveType;
	private Class<Q> returnType;
	private EndPoint<Q,P> callback;
	
	public EndpointBuilder() {
		this(null);
	}
	
	@SuppressWarnings("unchecked")
	public EndpointBuilder(String endpoint) {
		this.endpoint = endpoint;
		this.setHttpMethod(HttpMethod.GET);
		this.setConsumes(MediaType.ALL);
		this.setProduces(MediaType.ALL);
		this.setReceiveType((Class<P>) Object.class);
		this.setReturnType((Class<Q>) Object.class);
	}
	
	public EndpointBuilder<P,Q> setEndpoint(String endpoint) {
		this.endpoint = endpoint;
		return this;
	}
	
	public EndpointBuilder<P,Q> setReceiveType(Class<P> clazz) {
		this.receiveType = clazz;
		return this;
	}
	
	public EndpointBuilder<P,Q> setReturnType(Class<Q> clazz) {
		this.returnType = clazz;
		return this;
	}

	public EndpointBuilder<P,Q> setProduces(MediaType type) {
		this.produces = type;
		return this;
	}

	public EndpointBuilder<P,Q> setConsumes(MediaType type) {
		this.consumes = type;
		return this;
	}

	public EndpointBuilder<P,Q> setHttpMethod(HttpMethod method) {
		this.httpMethod = method;
		return this;
	}
	
	public EndpointBuilder<P,Q> setOnRequest(EndPoint<Q, P> callback) {
		this.callback = callback;
		return this;
	}

	public String getEndpoint() {
		return this.endpoint;
	}

	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	public MediaType getConsumes() {
		return this.consumes;
	}

	public MediaType getProduces() {
		return this.produces;
	}

	public Class<P> getReceiveType() {
		return this.receiveType;
	}

	public Class<Q> getReturnType() {
		return this.returnType;
	}

	protected EndPoint<Q,P> getRequest() {
		return callback;
	}
}
