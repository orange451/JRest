# JRest
Small and Lightweight Rest Library. Very barebones.

Simple GET request:
```java
// Send request
RequestEntity<String> request = new RequestEntity<>(HttpMethod.GET);
ResponseEntity<String> response = request.exchange("http://localhost/testAPI", String.class);
System.out.println(response.getBody());
```

Simple GET request (async):
```java
RequestEntity<JsonObject> request = new RequestEntity<>(HttpMethod.GET);
request.exchangeAsync("http://localhost/testJson", JsonObject.class, (response)->{
	System.out.println(response.getBody());
});
```

Simple Rest Server:
```java
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
```
