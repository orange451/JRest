package io.jrest;

@FunctionalInterface
public interface EndPoint<Q,P> {
	public ResponseEntity<Q> run(HttpRequest<P> request);
}
