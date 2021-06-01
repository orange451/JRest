# JRest
Super small and lightweight Java REST Library. **Only has one (optional) dependency, Gson (For POJO Serialization)!** JRest doesn't use annotations, doesn't require polymorphism, and *can* work asynchrounously. If JRest is ran without the Gson dependency, DTO/POJO objects cannot be serialized/deserialized but JSON Objects can still be serialized to maps using Javas' native Nashorn Javascript engine.

# Why JRest?
Many Java Rest libraries claim to be lightweight. How many require 0 dependencies, start up in less than 50 ms, are less than 50 kb in filesize, and only have 1 package? Enter JRest, the **real** lightweight Java Restful library. It can be used as a webhost, back-end server, or to make requests to already existing REST endpoints. 

# Examples
Simple GET request:
```java
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
public class TestServer {

	public static void main(String[] args) {
		/**
		 * Start server
		 */
		JRest server = JRest.create()
				.setServerName("Test Server")
				.setPort(80)
				.start();
		
		/**
		 * Open in a web browser! http://localhost/
		 */		
		server.addEndpoint(HttpMethod.GET, "/", MediaType.TEXT_HTML, (request)->{
			return new ResponseEntity<String>(HttpStatus.OK, "<h1>Index! Welcome to JREST!</h1>");
		});
		
		/**
		 * Test Endpoint. Returns static String
		 */
		server.addEndpoint(HttpMethod.GET, "/testAPI", (request)->{
			return new ResponseEntity<String>(HttpStatus.OK, "Hello From Server!");
		});
	}
}
```

Serialize Maps to JsonObjects:
```Java
/**
 * SERVER CODE
 */
server.addEndpoint(HttpMethod.POST, "/GetEmployee", JsonObject.class, (request)->{
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

   public static void main(String[] args) throws MalformedURLException, IOException {
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
server.addEndpoint(HttpMethod.GET, "/", MediaType.TEXT_HTML, (request)->{
   return new ResponseEntity<String>(HttpStatus.OK, "<h1>Index! Welcome to JREST!</h1>");
});
```
![testImage](https://i.imgur.com/jrYyeFv.png)

Statistics:
- JRest library classes package to less than 50KB in filesize
- JRest servers can boot in less than 50 ms
- Does not inherently require dependencies outside of the JDK/JRE
- Average response time of "Hello World" get request is 3-4 ms (localhost).
