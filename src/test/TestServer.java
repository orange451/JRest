package test;

import com.google.gson.JsonObject;
import jrest.HttpMethod;
import jrest.HttpStatus;
import jrest.MediaType;
import jrest.ResponseEntity;
import jrest.RestServer;

public class TestServer extends RestServer {
	
	String[] names = {
			"Frank",
			"Jeff",
			"Oliver",
			"Maxwell"
	};
	
	public TestServer() {
		
		/**
		 * Test Endpoint. Returns static String
		 */
		this.addEndpoint(HttpMethod.GET, "/", MediaType.TEXT_HTML, (request)->{
			return new ResponseEntity<String>(HttpStatus.OK, "<h1>Index! Welcome to JREST!</h1>");
		});

		
		/**
		 * Test Endpoint. Returns static String
		 */
		this.addEndpoint(HttpMethod.GET, "/testAPI", (request)->{
			return new ResponseEntity<String>(HttpStatus.OK, "Hello From Server!");
		});
		
		/**
		 * Test Post endpoint. Returns your posted data back to you.
		 */
		this.addEndpoint(HttpMethod.POST, "/GetEmployee", JsonObject.class, (request)->{
			JsonObject payload = request.getBody();
			int id = payload.get("id").getAsInt();
			
			JsonObject response = new JsonObject();
			response.addProperty("id", id);
			response.addProperty("name", names[id-1]);
			
			return new ResponseEntity<JsonObject>(HttpStatus.OK, response);
		});
		
		/**
		 * Test Post endpoint. Returns your posted data back to you.
		 */
		this.addEndpoint(HttpMethod.GET, "/GetUsername", (request)->{
			int id = Integer.parseInt(request.getUrlParameters().get("id").toString());
			String name = names[id-1];
			return new ResponseEntity<String>(HttpStatus.OK, name);
		});
		
		/**
		 * Test JSON endpoint. Returns a JSON object.
		 */
		this.addEndpoint(HttpMethod.GET, "/testJson", MediaType.ALL, MediaType.APPLICATION_JSON, (request)->{
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("TestKey", "Hello World!");
			
			return new ResponseEntity<JsonObject>(HttpStatus.OK, jsonObject);
		});
	}
	
	@Override
	public int getPort() {
		return 80;
	}

	public static void main(String[] args) {
		new TestServer();
	}
}