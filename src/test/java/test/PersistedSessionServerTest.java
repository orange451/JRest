package test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.jrest.HttpMethod;
import io.jrest.HttpSession;
import io.jrest.HttpStatus;
import io.jrest.JRest;
import io.jrest.MediaType;
import io.jrest.ResponseEntity;
import io.jrest.Logger.LogType;

public class PersistedSessionServerTest {
	
	public static void main(String[] args) {
		/**
		 * Start server
		 */
		JRest server = JRest.create()
				.setServerName("Test Server")
				.setLogType(LogType.TRACE)
				.setPort(80)
				.start();
		
		/** Load stored sessions from file **/
		JRest.getSessionStorage().deserializeSessions(loadSessions());

		/**
		 * Session test!
		 */
		server.addEndpoint(HttpMethod.GET, "/testSession", MediaType.ALL, (request)->{
			HttpSession session = request.session();
			
			String text = "Value of session.TESTKEY = " + session.get("TESTKEY");
			session.put("TESTKEY", "Hello World!");
			
			/** Write all sessions to file for storage **/
			writeSessions(JRest.getSessionStorage().serializeSessions());
			// TODO replace this with a more elegant solution. No need to write every time the endpoint is called.
			
			return new ResponseEntity<String>(HttpStatus.OK, text);
		});
	}
	
	/**
	 * Load serialized sessions json file
	 */
	private static String loadSessions() {
		try {
			byte[] data = Files.readAllBytes(Paths.get("sessions.json"));
			return new String(data, StandardCharsets.UTF_8);
		} catch (IOException e) {
			//
		}
		
		return "{}";
	}
	
	/**
	 * Write json to sessions json file
	 */
	private static void writeSessions(String json) {
		try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter("sessions.json"));
		    writer.write(json);
		    writer.close();
		} catch(Exception e) {
			//
		}
	}
}