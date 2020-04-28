package jrest;

@FunctionalInterface
public interface AsyncResponse<T> {
	public void response(ResponseEntity<T> response);
}
