package test;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;

import com.google.gson.JsonObject;

import jrest.HttpMethod;
import jrest.RequestEntity;

public class TestCookies {
	public static void main(String[] args) throws MalformedURLException, IOException {
		// Create request object with cookie
		RequestEntity<JsonObject> request = new RequestEntity<>(HttpMethod.GET);
		request.getCookies().add(new HttpCookie("HeyImCookie", "DontDeletePls"));
		
		// Send request (with cookie) to server
		request.exchangeAsync("http://localhost/", String.class, (response)->{
			System.err.println("Received cookies (1): " + response.getCookies());
		});
		
		// Send second request, this one should also contain cookie, but we don't have to resend it!
		new RequestEntity<>(HttpMethod.GET).exchangeAsync("http://localhost/", String.class, (response)->{
			System.err.println("Received cookies (2): " + response.getCookies());
		});
	}
}