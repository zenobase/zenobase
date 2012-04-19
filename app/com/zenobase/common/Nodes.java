package com.zenobase.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.io.JsonPrinter;

public class Nodes {

	private static final ObjectMapper mapper = new ObjectMapper();

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
			return (T) mapper.readTree(node.traverse());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public static byte[] toByteArray(ObjectNode node) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			new JsonPrinter(out).print(node);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return out.toByteArray();
	}

	public static ObjectNode read(byte[] in) {
		try {
			return (ObjectNode) mapper.readTree(in);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
