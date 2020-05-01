package test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.google.gson.JsonObject;

import jrest.HttpMethod;
import jrest.RequestEntity;

public class TestURLParams {

	public TestURLParams() throws MalformedURLException {
		// Create request object
		RequestEntity<JsonObject> request = new RequestEntity<>(HttpMethod.GET);
		
		// Send request to server
		request.exchangeAsync("http://localhost/GetUsername?id=3", String.class, (response)->{
			String name = response.getBody();
			System.out.println("User name: " + name);
		});
	}

	public static void main(String[] args) throws MalformedURLException, IOException {
		new TestURLParams();
	}
}