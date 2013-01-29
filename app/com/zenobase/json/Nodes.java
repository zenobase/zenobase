package com.zenobase.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;

public class Nodes {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private Nodes() {

	}

	public static ObjectNode newObject() {
		return MAPPER.createObjectNode();
	}

	public static ObjectNode newObject(String fieldName, String value) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkNotNull(value);
		ObjectNode node = newObject();
		node.put(fieldName, value);
		return node;
	}

	public static ArrayNode newArray() {
		return MAPPER.createArrayNode();
	}

	public static ArrayNode newArray(Iterable<String> values) {
		ArrayNode result = newArray();
		for (String value : values) {
			result.add(value);
		}
		return result;
	}

	public static <T extends JsonNode> T copy(T node) {
		try {
			return (T) MAPPER.readTree(node.traverse());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public static byte[] toByteArray(JsonNode node) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			MAPPER.writer().writeValue(out, node);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return out.toByteArray();
	}

	public static ObjectNode readObject(byte[] in) {
		return (ObjectNode) read(in);
	}

	public static ArrayNode readArray(byte[] in) {
		return (ArrayNode) read(in);
	}

	private static JsonNode read(byte[] in) {
		try {
			return MAPPER.readTree(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("Can't read json: '" + new String(in) + "'");
		}
	}

    public static ObjectNode readObject(String in) {
        return (ObjectNode) read(in);
    }

    public static ArrayNode readArray(String in) {
        return (ArrayNode) read(in);
    }

    private static JsonNode read(String in) {
        try {
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read json: '" + new String(in) + "'");
        }
    }

	public static int size(JsonNode node) {
		return node != null ? node.size() : 0;
	}
}
