package io.jrest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionStorage {
	private Map<String, HttpSession> storage = new HashMap<>();
	
	/**
	 * Get a session by its uuid string.
	 */
	public HttpSession get(String uuid) {
		return storage.get(uuid);
	}
	
	/**
	 * Create a new HttpSession and add it to the storage.
	 */
	public HttpSession create() {
		HttpSession session = new HttpSession();
		storage.put(session.getUUID().toString(), session);
		return session;
	}
	
	/**
	 * Returns all active sessions. See {@link HttpSession#isValid()}.
	 */
	public List<HttpSession> getSessions() {
		List<HttpSession> sessions = new ArrayList<>();
		
		for (HttpSession session : storage.values()) {
			if ( !session.isValid() )
				continue;
			
			sessions.add(session);
		}
		
		return sessions;
	}
	
	/**
	 * Loads a list of sessions in to session storage.
	 */
	public void loadSessions(List<HttpSession> sessions) {
		for (HttpSession session : sessions) {
			if ( storage.containsKey(session.getUUID().toString()) )
				continue;
			
			storage.put(session.getUUID().toString(), session);
		}
	}
	
	/**
	 * returns all active sessions serialized to a json string. Useful for writing to a file.
	 */
	public String serializeSessions() {
		return SessionUtil.serializeAll(getSessions());
	}

	/**
	 * Loads all session objects from json and loads them in to the session storage.
	 */
	public void deserializeSessions(String serializedJson) {
		List<HttpSession> list = SessionUtil.deserializeAll(serializedJson);
		if ( list == null )
			return;
		
		loadSessions(list);
	}
}