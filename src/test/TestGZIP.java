package test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.google.gson.JsonObject;

import io.jrest.HttpHeaders;
import io.jrest.HttpMethod;
import io.jrest.RequestEntity;

public class TestGZIP {
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		// Create request object
		RequestEntity<JsonObject> request = new RequestEntity<>(HttpMethod.GET, new HttpHeaders().setContentEncoding("gzip"));
		
		// Send request to server
		request.exchangeAsync("http://localhost/testGZIP", String.class, (response)->{
			System.out.println(response.getBody());
		});
	}
}