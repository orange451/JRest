package test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jrest.HttpHeaders;
import jrest.HttpMethod;
import jrest.MediaType;
import jrest.RequestEntity;
import jrest.ResponseEntity;

public class TestClient {
	public TestClient() throws MalformedURLException, IOException {
		
		// Send request
		/*RequestEntity<String> request1 = new RequestEntity<>(HttpMethod.GET);
		ResponseEntity<String> response1 = request1.exchange("http://localhost/testAPI", String.class);
		System.out.println(response1.getBody());
		
		// Test JSON
		RequestEntity<JsonObject> request2 = new RequestEntity<>(HttpMethod.GET);
		request2.exchangeAsync("http://localhost/testJson", JsonObject.class, (response)->{
			System.out.println(response.getBody());
		});
		
		// Test POST JSON
		JsonObject body = new JsonObject();
		body.addProperty("TestKey", "The server will send this back to us!");
		RequestEntity<JsonObject> request3 = new RequestEntity<>(HttpMethod.POST, body);
		request3.exchangeAsync("http://localhost/testPost", JsonObject.class, (response)->{
			System.out.println(response.getBody());
		});*/

		// Test website endpoint
		RequestEntity<String> request4 = new RequestEntity<>(HttpMethod.GET);
		request4.exchangeAsync("https://jsonplaceholder.typicode.com/todos/1", String.class, (response)->{
			System.out.println(response.getBody());
		});
		
		/*HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.put("Cache-Control", "no-cache");
		headers.put("Host", "osrs-map.herokuapp.com");
		headers.put("Origin", "https://explv.github.io");
		
		String json = "" +
				"{" +
				"    \"start\": {" + 
				"        \"x\": 2959," + 
				"        \"y\": 3204," + 
				"        \"z\": 0" + 
				"    }," + 
				"    \"end\": {" + 
				"        \"x\": 3300," + 
				"        \"y\": 3211," + 
				"        \"z\": 0" + 
				"    }" +
				"}";
		JsonObject body = (JsonObject) new JsonParser().parse(json);

		new RequestEntity<JsonObject>(HttpMethod.POST, headers, body).exchangeAsync("https://osrs-map.herokuapp.com/getPath", JsonObject.class, (response)->{
			System.out.println(response.getBody());
		});*/
	}

	public static void main(String[] args) throws MalformedURLException, IOException {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		
		new TestClient();
	}
}