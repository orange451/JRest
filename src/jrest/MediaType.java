package jrest;

public class MediaType {
	
	public static final String ALL_VALUE = "*/*";
	public static final MediaType ALL = new MediaType(ALL_VALUE);
	
	public static final String APPLICATION_JSON_VALUE = "application/json";
	public static final MediaType APPLICATION_JSON = new MediaType(APPLICATION_JSON_VALUE);
	
	public static final String TEXT_PLAIN_VALUE = "text/plain";
	public static final MediaType TEXT_PLAIN = new MediaType(TEXT_PLAIN_VALUE);
	
	private String type;
	
	MediaType(String type) {
		this.type = type;
	}
	
	public String toString() {
		return this.type;
	}
	
	public String getType() {
		return this.type;
	}
}
