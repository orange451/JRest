package io.jrest;

public abstract class Marshaller {
	public abstract <T> T parse(String body, T type);
	
	public abstract String stringify(Object body);
}
