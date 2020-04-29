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
		this.addEndpoint(HttpMethod.POST, "/GetEmployee", JsonObject.class, (request)->{
			JsonObject payload = (JsonObject) request.getBody();
			int id = payload.get("id").getAsInt();
			
			String[] names = {
					"Frank",
					"Jeff",
					"Oliver",
					"Maxwell"
			};
			
			JsonObject response = new JsonObject();
			response.addProperty("id", id);
			response.addProperty("name", names[id-1]);
			
			return new ResponseEntity<JsonObject>(HttpStatus.OK, response);
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