package jrest;

@FunctionalInterface
public interface EndPoint {
	public ResponseEntity<?> run(HttpRequest<?> request);
}
