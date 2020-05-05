package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import jrest.HttpMethod;
import jrest.RequestEntity;

public class TestCookies {
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		URL endpoint = new URL("http://localhost/testCookie");
		
		// Send request to server, server will give us a cookie.
		new RequestEntity<>(HttpMethod.GET).exchangeAsync(endpoint, (response)->{
			System.out.println("Received cookies (1): " + response.getCookies());
			System.out.println("Received message (2): " + response.getBody());
			
			// Send second request, this will automatically send back cookie sent from server.
			new RequestEntity<>(HttpMethod.GET).exchangeAsync(endpoint, (response2)->{
				System.out.println("Received message (3): " + response2.getBody());
			});
		});
	}
}