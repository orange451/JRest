package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import io.jrest.HttpMethod;
import io.jrest.RequestEntity;

public class TestCookies {
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		URL endpoint = new URL("http://localhost/testCookie");
		
		/**
		 * Send first request to endpoint. This endpoint should respond with a cookie.
		 */
		new RequestEntity<>(HttpMethod.GET).exchangeAsync(endpoint, (response)->{
			System.out.println("Received message (1): " + response.getBody());
			System.out.println("Received cookies (2): " + response.getCookies());
			
			/**
			 * Second request. Server sent us cookie in first request, so we will automatically send it back in second one.
			 * Server is programmed to respond with a different message when it receives a cookie.
			 */
			new RequestEntity<>(HttpMethod.GET).exchangeAsync(endpoint, (response2)->{
				System.out.println("Received message (3): " + response2.getBody());
			});
		});
	}
}