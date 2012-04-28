package com.zenobase.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

public class Nodes {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private Nodes() {

	}

	public static ObjectNode newObject() {
		return JsonNodeFactory.instance.objectNode();
	}

	public static ArrayNode newArray() {
		return JsonNodeFactory.instance.arrayNode();
	}

	public static <T extends JsonNode> T copy(T node) {
		try {
			return (T) MAPPER.readTree(node.traverse());
		} catch (IOException e) {
			throw new AssertionError();
		}
	}

	public static byte[] toByteArray(ObjectNode node) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			MAPPER.writer().writeValue(out, node);
		} catch (IOException e) {
			throw new AssertionError();
		}
		return out.toByteArray();
	}

	public static ObjectNode read(byte[] in) {
		try {
			return (ObjectNode) MAPPER.readTree(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("Can't read json: '" + new String(in) + "'");
		}
	}
}
