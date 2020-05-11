package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;

import io.jrest.HttpHeaders;
import io.jrest.HttpMethod;
import io.jrest.MediaType;
import io.jrest.RequestEntity;

public class TestFormUrlEncoded {
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		
		// Define Headers (This is how it'll know to use params in url not body)
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		// Define params
		Map<String, String> map = new LinkedHashMap<>();
		map.put("id", "123");
		
		// Create request object
		RequestEntity<Map<String, String>> request = new RequestEntity<>(HttpMethod.POST, headers, map);
		
		// Send request to server
		request.exchangeAsync("http://localhost/testForm", String.class, (response)->{
			String name = response.getBody();
			System.out.println("User name: " + name);
		});
	}
}