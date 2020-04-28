package test;

import com.google.gson.JsonObject;
import jrest.HttpMethod;
import jrest.HttpStatus;
import jrest.MediaType;
import jrest.ResponseEntity;
import jrest.RestServer;

public class TestServer extends RestServer {
	public TestServer() {
		
		/**
		 * Test Endpoint. Returns static String
		 */
		this.addEndpoint(HttpMethod.GET, "/testAPI", (request)->{
			return new ResponseEntity<String>(HttpStatus.OK, "Hello From Server!");
		});
		
		/**
		 * Test Post endpoint. Returns your posted data back to you.
		 */
		this.addEndpoint(HttpMethod.POST, "/testPost", MediaType.ALL, MediaType.ALL, (request)->{
			return new ResponseEntity<String>(HttpStatus.OK, request.getBody().toString());
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