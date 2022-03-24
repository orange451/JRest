package io.jrest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RestUtil {
	
	private static boolean canUseGson;
	
	private static final Set<String> ignoreCustomHeaders;
	
	private static MarshallerNashorn nashorn;
	
	private static MarshallerGson gson;

	static {
		ignoreCustomHeaders = new HashSet<>();
		ignoreCustomHeaders.add(HttpHeaders.CONTENT_TYPE);
		ignoreCustomHeaders.add("Content-Length");
		
		try {
			nashorn = new MarshallerNashorn();
			gson = new MarshallerGson();
			gson.parse(null, null);
			
			canUseGson = true;
		} catch (Exception e) {
			System.err.println("Could not locate Gson dependency, will not serialize Java classes to DTO/POJO. Using Nashorn engine as fallback Map/List serializer.");
		}
	}

	/**
	 * Attempts to serialize an object (Map, List, POJO, String) to a string.
	 */
	protected static String convertToString(Object object) {
		if (object instanceof String)
			return object.toString();

		if (canUseGson)
			return gson.stringify(object);

		// Oh boy manual json serialization...
		if (object instanceof Map || object instanceof List)
			return StringUtil.serializeJson(object);

		// Fallback
		return object.toString();
	}

	/**
	 * Attempt to deserialize a string in to a specified type.
	 */
	@SuppressWarnings("unchecked")
	protected static <T> T convertToObject(String bodyString, T type) {
		if (bodyString == null || bodyString.length() == 0)
			return (T) null;

		Class<?> c = (Class<?>) type;
		
		// Try gson
		if (canUseGson) {
			T result = gson.parse(bodyString, type);
			if ( result != null )
				return result;
		}
		
		// Try nashorn
		T result = nashorn.parse(bodyString, type);
		if ( result != null )
			return result;
		
		// Convert to String if its the type
		if (String.class.isAssignableFrom(c)) {
			return (T) bodyString.toString();
		}
		
		// If we can't convert, we must return null.
		return (T) null;
	}

	/**
	 * Read all data of an input stream and return a byte array.
	 */
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
		if ( connection.getContentEncoding() != null && connection.getContentEncoding().contains("gzip") ) {
			inputStream = new GZIPInputStream(inputStream);
		} else if ( connection.getContentEncoding() != null && connection.getContentEncoding().contains("br") ) {
			throw new RuntimeException("Cannot decode payload. Brotli decoding is not natively supported by Java. Please use a supported Accept-Encoding header parameter.");
		}
			
		// Read body
		String body = StringUtil.utf8(RestUtil.readAll(inputStream));

		// Update cookies
		List<String> cookiesHeader = map.get("Cookie");
		if (cookiesHeader != null) {
			for (String cookie : cookiesHeader) {
				List<HttpCookie> cookies = HttpCookie.parse(cookie);
				for (HttpCookie hcookie : cookies) {
					JRest.cookieManager.getCookieStore().add(null, hcookie);
				}
			}
		}

		// Create response object
		T tBody = RestUtil.convertToObject(body, type);
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

	/**
	 * Write http message to a socket.
	 */
	public static void write(Socket socket, String serverName, HttpStatus status, MediaType produces, String body, HttpHeaders headers, List<HttpCookie> cookiesList) throws IOException {
		Map<String, String> defaultHeaders = new HashMap<>();
		defaultHeaders.put(HttpHeaders.KEEP_ALIVE, "timeout=5, max=99");
		defaultHeaders.put(HttpHeaders.SERVER, serverName);
		defaultHeaders.put(HttpHeaders.CONNECTION, "Keep-Alive");
		
		if ( headers != null ) {
			for (Entry<String, String> set : headers.entrySet()) {
				if ( ignoreCustomHeaders.contains(set.getKey()) ) 
					continue;
				
				defaultHeaders.put(set.getKey(), set.getValue());
			}
		}
		
		// Dont support Brotli
		if ( defaultHeaders.get(HttpHeaders.CONTENT_ENCODING) != null && defaultHeaders.get(HttpHeaders.CONTENT_ENCODING).contains("br") )
			throw new RuntimeException("Cannot write data. Brotli encoding is not natively supported by Java. Please use a different encoding parameter.");
		
		// Write http status
		OutputStream outputStream = socket.getOutputStream();
		BufferedOutputStream b = new BufferedOutputStream(outputStream);
		b.write(StringUtil.utf8("HTTP/1.1 " + status.value() + " " + status.getReasonPhrase() + "\n"));
		
		// Write headers
		for (Entry<String, String> set : defaultHeaders.entrySet()) {
			String header = set.getKey() + ": " + set.getValue();
			b.write(StringUtil.utf8(header + "\n"));
		}
		
		// Write cookies to user
		if (cookiesList != null && cookiesList.size() > 0) {
			for (HttpCookie cookie : cookiesList) {
				String cookieHeader = new String("Set-Cookie: " + cookie + "\n");
				b.write(StringUtil.utf8(cookieHeader));
			}
		}
		
		// Get final body
		byte[] finalBody = null;
		if ( defaultHeaders.get(HttpHeaders.CONTENT_ENCODING) != null && defaultHeaders.get(HttpHeaders.CONTENT_ENCODING).contains("gzip") ) {
			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
			GZIPOutputStream gzipBodyStream = new GZIPOutputStream(byteArrayOS);
			gzipBodyStream.write(StringUtil.utf8(body));
			gzipBodyStream.close();
			gzipBodyStream = null;
			
			finalBody = byteArrayOS.toByteArray();
		} else {
			finalBody = StringUtil.utf8(body);
		}
		
		// Write content predata
		b.write(StringUtil.utf8("Content-Length: " + finalBody.length + "\n"));
		b.write(StringUtil.utf8("Content-Type: " + produces.getType() + "\n"));
		
		// Tell the parser that we are going to begin writing data
		b.write(StringUtil.utf8("\n"));
		
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
