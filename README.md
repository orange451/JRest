# JRest
Small and Lightweight Java Rest Library. Very barebones.

Requires Gson Dependency

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

Serialize Maps to JsonObjects:
```Java
/**
 * SERVER CODE
 */
this.addEndpoint(HttpMethod.POST, "/GetEmployee", JsonObject.class, (request)->{
   JsonObject payload = request.getBody();
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
 * CLIENTCODE
 */
JsonObject body = new JsonObject();
body.addProperty("id", 1);
RequestEntity<JsonObject> request = new RequestEntity<>(HttpMethod.POST, body);
request.exchangeAsync("http://localhost/GetEmployee", JsonObject.class, (response)->{
   JsonObject payload = response.getBody();
   System.out.println("Employee data: ");
   System.out.println("\tid: " + payload.get("id").getAsInt());
   System.out.println("\tname: " + payload.get("name"));
});
```

Using DTO/POJO objects:
```Java
public class TestDTO {

   public TestDTO() throws MalformedURLException {
      // Create payload
      JsonObject body = new JsonObject();
      body.addProperty("id", 1);
      
      // Create request object
      RequestEntity<JsonObject> request = new RequestEntity<>(HttpMethod.POST, body);
      
      // Send request to server
      request.exchangeAsync("http://localhost/GetEmployee", Employee.class, (response)->{
         Employee employee = response.getBody();
         System.out.println("Employee data: ");
         System.out.println("\tid: " + employee.getId());
         System.out.println("\tname: " + employee.getName());
      });
   }

   public static void main(String[] args) throws MalformedURLException, IOException {
      new TestDTO();
   }
}

/**
 * DTO used to represent employee information sent from server.
 */
class Employee {
   private int id;
   
   private String name;
   
   public Employee() {
      //
   }
   
   public int getId() {
      return id;
   }
   
   public void setId(int id) {
      this.id = id;
   }
   
   public String getName() {
      return this.name;
   }
   
   public void setName(String name) {
      this.name = name;
   }
}
```

Host a webserver:
```Java
/**
 * SERVER CODE
 */
this.addEndpoint(HttpMethod.GET, "/", MediaType.TEXT_HTML, (request)->{
   return new ResponseEntity<String>(HttpStatus.OK, "<h1>Index! Welcome to JREST!</h1>");
});
```
![testImage](https://i.imgur.com/jrYyeFv.png)
