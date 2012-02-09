package common;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

public class JsonPrinter {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final OutputStream out;

	public JsonPrinter(OutputStream out) {
		this.out = out;
	}

	public void print(ObjectNode object) throws IOException {
		mapper.writerWithDefaultPrettyPrinter().writeValue(out, object);
	}
}
