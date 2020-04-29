package test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jrest.HttpMethod;
import jrest.RequestEntity;
import jrest.ResponseEntity;

public class TestClient {
	public TestClient() throws MalformedURLException, IOException {
		
		// Send request
		RequestEntity<String> request1 = new RequestEntity<>(HttpMethod.GET);
		ResponseEntity<String> response1 = request1.exchange("http://localhost/testAPI", String.class);
		System.out.println(response1.getBody());
		
		// Test JSON
		RequestEntity<JsonObject> request2 = new RequestEntity<>(HttpMethod.GET);
		request2.exchangeAsync("http://localhost/testJson", JsonObject.class, (response)->{
			System.out.println(response.getBody());
		});
		
		// Test POST JSON
		JsonObject body = new JsonObject();
		body.addProperty("id", 1);
		RequestEntity<JsonObject> request3 = new RequestEntity<>(HttpMethod.POST, body);
		request3.exchangeAsync("http://localhost/GetEmployee", JsonObject.class, (response)->{
			JsonObject payload = response.getBody();
			System.out.println("Employee data: ");
			System.out.println("\tid: " + payload.get("id").getAsInt());
			System.out.println("\tname: " + payload.get("name"));
		});

		// Test website endpoint
		RequestEntity<JsonArray> request4 = new RequestEntity<>(HttpMethod.GET);
		request4.exchangeAsync("http://robloxwwii.com/RobloxServerList.php", JsonArray.class, (response)->{
			System.out.println(response.getBody());
		});
	}

	public static void main(String[] args) throws MalformedURLException, IOException {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		
		new TestClient();
	}
}