package common;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonPrinter {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final OutputStream out;

	public JsonPrinter(OutputStream out) {
		this.out = out;
	}

	public void print(JsonNode object) throws IOException {
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
