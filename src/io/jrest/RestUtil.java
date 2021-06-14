package io.jrest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class RestUtil {
	private static Gson gson;

	private static boolean canUseGson;

	private static ScriptEngine engine;

	static {
		try {
			gson = new GsonBuilder().serializeNulls().create();
			canUseGson = true;
		} catch (NoClassDefFoundError e) {
			System.err.println("Could not locate Gson dependency, will not serialize Java classes. Using Nashorn engine as fallback Map/List serializer.");

			ScriptEngineManager sem = new ScriptEngineManager();
			engine = sem.getEngineByName("javascript");
		}
	}

	/**
	 * Attempts to serialize an object (Map, List, POJO, String) to a string.
	 * 
	 * @param object
	 * @return
	 */
	protected static String convertSoString(Object object) {
		if (object instanceof String)
			return object.toString();

		if (canUseGson)
			return RestUtil.gson.toJson(object);

		// Oh boy manual json serialization...
		if (object instanceof Map || object instanceof List)
			return serializeJson(object);

		// Fallback
		return object.toString();
	}

	/**
	 * Manual json serialization. Only used if Gson is not available.
	 * 
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static String serializeJson(Object object) {

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

	private static String getSerializedJSONValue(Object value) {
		if (value == null)
			return "null";
		else if (value instanceof List)
			return serializeJson(value);
		else if (value instanceof Map)
			return serializeJson(value);
		else if (value instanceof Number || value instanceof Boolean)
			return ((Number) value).toString();
		else if (value instanceof String)
			return "\"" + value.toString() + "\"";

		// Cant be serialized
		return "null";
	}

	@SuppressWarnings("unchecked")
	protected static <T> T convertObject(String bodyString, T type) {
		if (bodyString == null || bodyString.length() == 0)
			return (T) null;

		Class<?> c = (Class<?>) type;

		if (canUseGson) {
			// Convert to gson tree
			if (JsonObject.class.isAssignableFrom(c)) {
				Type empMapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				return (T) gson.toJsonTree(gson.fromJson(bodyString, empMapType)).getAsJsonObject();
			}

			// json array
			if (JsonArray.class.isAssignableFrom(c)) {
				Type empMapType = new TypeToken<List<Object>>() {
				}.getType();
				Object obj = gson.fromJson(bodyString, empMapType);
				return (T) gson.toJsonTree(obj).getAsJsonArray();
			}

			// Convert to map
			if (Map.class.isAssignableFrom(c)) {
				Type empMapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				return gson.fromJson(bodyString, empMapType);
			}

			// Convert to list
			if (List.class.isAssignableFrom(c)) {
				Type empMapType = new TypeToken<List<Object>>() {
				}.getType();
				return gson.fromJson(bodyString, empMapType);
			}
		} else {
			// Super ugly hack using Javax Nashorn js library. We can only reliably get Maps
			// or Lists.
			String script = "Java.asJSONCompatible(" + bodyString + ")";
			try {
				Object result = engine.eval(script);
				if (result instanceof Map)
					return (T) ((Map<?, ?>) result);
				if (result instanceof List)
					return (T) ((List<?>) result);
			} catch (ScriptException e) {
				System.err.println("Failed to parse " + script);
				e.printStackTrace();
			}
		}

		// Convert to String
		if (String.class.isAssignableFrom(c)) {
			return (T) bodyString.toString();
		}

		// Try to parse DTO as fallback
		if (canUseGson) {
			try {
				return (T) gson.fromJson(bodyString, c);
			} catch (Exception e) {
				return null;
			}
		}
		
		// If all else fails, return the body as a string
		return (T) bodyString.toString();
	}

	protected static byte[] readAll(InputStream inputStream) throws IOException {
		long TIMEOUT = System.currentTimeMillis() + 2000;

		// Wait until ready
		BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
		while (bufferedInput.available() == 0) {
			if (System.currentTimeMillis() > TIMEOUT) {
				return null;
			}
		}
		
		byte[] totalData = new byte[bufferedInput.available()];
		bufferedInput.read(totalData);
		
		// GZIP has this stupid available implementation which only gives us 1 byte at a time...
		List<byte[]> extraData = new ArrayList<>();
		int extraBytesLen = 0;
		while(bufferedInput.available() > 0 ) {
			extraBytesLen += bufferedInput.available();
			byte[] newData = new byte[bufferedInput.available()];
			bufferedInput.read(newData);
			extraData.add(newData);
		}
		
		// Return data
		if ( extraData.size() == 0 ) {
			return totalData;
		} else {
			
			// Iterate over all the data inputs, and combine.
			extraData.add(0, totalData);
			
			byte[] ret = new byte[extraBytesLen + totalData.length];
			int t = 0;
			for(byte[] data : extraData) {
				for (int i=0; i<data.length; i++) {
					ret[t++] = data[i];
				}
			}
			
			return ret;
		}
	}

	/**
	 * Reads a connection-stream and parses into HttpResponse object.
	 * @throws IOException
	 */
	protected static <T> HttpResponse<T> readResponse(HttpURLConnection connection, T type) throws IOException {
		// Create response headers
		HttpHeaders headers = new HttpHeaders();
		Map<String, List<String>> map = connection.getHeaderFields();
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			String vals = "";
			for (String val : entry.getValue()) {
				if ( vals.length() > 0 )
					val += ", ";
				
				vals += val;
			}
			headers.put(entry.getKey(), vals);
		}
		
		// Grab input stream
		InputStream inputStream = connection.getInputStream();
		if ( connection.getContentEncoding() != null && connection.getContentEncoding().contains("gzip") )
			inputStream = new GZIPInputStream(inputStream);
		else if ( connection.getContentEncoding() != null && connection.getContentEncoding().contains("br") )
			throw new RuntimeException("Cannot decode payload. Brotli decoding is not natively supported by Java. Please use a supported Accept-Encoding header parameter.");
		
		// Read body
		String body = utf8(RestUtil.readAll(inputStream));

		// Update cookies
		List<String> cookiesHeader = map.get("Cookie");
		if (cookiesHeader != null) {
			for (String cookie : cookiesHeader) {
				JRest.cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
			}
		}

		// Create response object
		T tBody = RestUtil.convertObject(body, type);
		HttpResponse<T> request = new HttpResponse<>(HttpStatus.valueOf(connection.getResponseCode()), headers, tBody);
		request.cookies = new ArrayList<>(JRest.cookieManager.getCookieStore().getCookies());

		// Return
		return request;
	}
	
	/**
	 * Reads an input stream until it is no longer available.
	 * Returns a list of Strings representing the request.
	 * This list represents the header, body, and all other data sent during the request.
	 * @throws IOException
	 */
	protected static List<String> readRequestData(InputStream inputStream) throws IOException {
		long TIMEOUT = System.currentTimeMillis() + 1000;

		/** TODO Replace this with {@link #readAll(InputStream)} */
		BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
		while (bufferedInput.available() == 0) {
			if (System.currentTimeMillis() > TIMEOUT) {
				bufferedInput.close();
				return Arrays.asList();
			}
		}

		// Parse input into strings
		List<String> headerData = new ArrayList<String>();
		boolean buildingHeader = true;
		StringBuilder builder = new StringBuilder();
		while (bufferedInput.available() > 0) {
			int c = bufferedInput.read();
			char ch = (char) (c);
			if (ch == '\r')
				continue;

			if (ch == '\n' && buildingHeader) {
				if (builder.length() == 0)
					buildingHeader = false;

				if (buildingHeader) {
					headerData.add(builder.toString().trim());
					builder.setLength(0);
				}
			} else {
				builder.append(ch);
			}
		}

		// BODY IS LAST
		headerData.add(builder.toString());
		return headerData;
	}
	
	private static byte[] utf8(String string) throws UnsupportedEncodingException {
		if ( string == null )
			return new byte[0];
		
		return string.getBytes("UTF-8");
	}
	
	private static String utf8(byte[] data) {
		if ( data == null )
			return "";
		
		return new String(data, Charset.forName("UTF-8"));
	}

	public static void write(Socket socket, String serverName, HttpStatus status, MediaType produces, String body, HttpHeaders headers, List<HttpCookie> cookiesList) throws IOException {
		Map<String, String> defaultHeaders = new HashMap<>();
		defaultHeaders.put(HttpHeaders.KEEP_ALIVE, "timeout=5, max=99");
		defaultHeaders.put(HttpHeaders.SERVER, serverName);
		defaultHeaders.put(HttpHeaders.CONNECTION, "Keep-Alive");
		
		if ( headers != null ) {
			for (Entry<String, String> set : headers.entrySet()) {
				defaultHeaders.put(set.getKey(), set.getValue());
			}
		}
		
		// Dont support Brotli
		if ( defaultHeaders.get(HttpHeaders.CONTENT_ENCODING) != null && defaultHeaders.get(HttpHeaders.CONTENT_ENCODING).contains("br") )
			throw new RuntimeException("Cannot write data. Brotli encoding is not natively supported by Java. Please use a different encoding parameter.");
		
		// Write http status
		OutputStream outputStream = socket.getOutputStream();
		BufferedOutputStream b = new BufferedOutputStream(outputStream);
		b.write(utf8("HTTP/1.1 " + status.value() + " " + status.getReasonPhrase() + "\n"));
		
		// Write headers
		for (Entry<String, String> set : defaultHeaders.entrySet()) {
			String header = set.getKey() + ": " + set.getValue();
			b.write(utf8(header + "\n"));
		}
		
		// Write cookies to user
		if (cookiesList != null && cookiesList.size() > 0) {
			List<String> cookies = new ArrayList<>();
			for (HttpCookie cookie : cookiesList)
				cookies.add(cookie.toString());
			String cookieHeader = new String("Set-Cookie: " + String.join(";", cookies) + "\n");
			b.write(utf8(cookieHeader));
		}
		
		// Get final body
		byte[] finalBody = null;
		if ( defaultHeaders.get(HttpHeaders.CONTENT_ENCODING) != null && defaultHeaders.get(HttpHeaders.CONTENT_ENCODING).contains("gzip") ) {
			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
			GZIPOutputStream gzipBodyStream = new GZIPOutputStream(byteArrayOS);
			gzipBodyStream.write(utf8(body));
			gzipBodyStream.close();
			gzipBodyStream = null;
			
			finalBody = byteArrayOS.toByteArray();
		} else {
			finalBody = utf8(body);
		}
		
		// Write content predata
		b.write(utf8("Content-Length: " + finalBody.length + "\n"));
		b.write(utf8("Content-Type: " + produces.getType() + "\n"));
		
		// Tell the parser that we are going to begin writing data
		b.write(utf8("\n"));
		
		// Write data
		b.write(finalBody);
		b.flush();
	}
	
	public static String escape(String string) {
		if ( string == null )
			return null;
		
		return string.replace("'", "\'").replace("\"", "\\\"").replace("`", "\\`");
	}
}
