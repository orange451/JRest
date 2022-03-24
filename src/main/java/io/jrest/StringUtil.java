package io.jrest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class StringUtil {
	
	/**
	 * Convert a string into a UTF-8 encoded byte array.
	 */
	public static byte[] utf8(String string) throws UnsupportedEncodingException {
		if ( string == null )
			return new byte[0];
		
		return string.getBytes("UTF-8");
	}
	
	/**
	 * Convert a byte array into a UTF-8 encoded string.
	 */
	public static String utf8(byte[] data) {
		if ( data == null )
			return "";
		
		return new String(data, Charset.forName("UTF-8"));
	}

	/**
	 * Naive string escaping function. Replaces single quotes, Double quotes, and Tildes with escaped characters.
	 */
	public static String escape(String string) {
		if ( string == null )
			return null;
		
		return string.replace("'", "\'").replace("\"", "\\\"").replace("`", "\\`");
	}
	
	/**
	 * Manual json serialization. Should only used if Gson is not available.
	 */
	@SuppressWarnings("unchecked")
	public static String serializeJson(Object object) {

		if (object instanceof Map) {
			String str = "{";
			boolean first = true;
			for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) object).entrySet()) {
				if (!first)
					str += ", ";
				str += getSerializedJSONValue(entry.getKey().toString()) + ": "
						+ getSerializedJSONValue(entry.getValue());
				first = false;
			}
			str += "}";
			return str;
		} else if (object instanceof List) {
			String str = "[";
			boolean first = true;
			for (Object obj : (List<Object>) object) {
				if (!first)
					str += ", ";
				str += getSerializedJSONValue(obj);
				first = false;
			}
			str += "]";
			return str;
		}

		return new String();
	}

	/**
	 * Returns the value associated with a json key as a String. Should only be used if gson is unavailable.
	 */
	public static String getSerializedJSONValue(Object value) {
		if (value == null)
			return "null";
		else if (value instanceof List)
			return serializeJson(value);
		else if (value instanceof Map)
			return serializeJson(value);
		else if (value instanceof Number || value instanceof Boolean)
			return value.toString();
		else if (value instanceof String)
			return "\"" + value.toString() + "\"";

		// Cant be serialized
		return "null";
	}
}
