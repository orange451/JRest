package jrest;

@FunctionalInterface
public interface EndPoint<T> {
	public ResponseEntity<T> run(HttpRequest<T> request);
}
