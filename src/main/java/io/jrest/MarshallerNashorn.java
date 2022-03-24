package io.jrest;

import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class MarshallerNashorn extends Marshaller {

	private static ScriptEngine engine;

	@SuppressWarnings("unchecked")
	@Override
	public <T> T parse(String body, T type) {
		if ( engine == null )
			engine = new ScriptEngineManager().getEngineByName("javascript");
		
		String script = "Java.asJSONCompatible(" + body + ")";
		try {
			Object result = engine.eval(script);
			if (result instanceof Map)
				return (T) ((Map<?, ?>) result);
			if (result instanceof List)
				return (T) ((List<?>) result);
		} catch (ScriptException e) {
			System.err.println("Failed to parse " + script);
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public String stringify(Object body) {
		return body.toString();
	}

}
