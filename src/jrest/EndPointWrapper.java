package jrest;

class EndPointWrapper {

	private EndPoint endpoint;
	private MediaType consumes;
	private MediaType produces;
	
	public EndPointWrapper(EndPoint endpoint, MediaType consumes, MediaType produces) {
		this.endpoint = endpoint;
		this.consumes = consumes;
		this.produces = produces;
	}

	public EndPoint getEndpoint() {
		return this.endpoint;
	}
	
	public MediaType getConsumes() {
		return this.consumes;
	}
	
	public MediaType getProduces() {
		return this.produces;
	}
}
