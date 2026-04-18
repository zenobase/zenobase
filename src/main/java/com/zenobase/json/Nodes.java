package com.zenobase.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class Nodes {

	public static final ObjectMapper MAPPER = JsonMapper.builder()
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
		.build();

	private Nodes() {}

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

	public static ObjectNode newObject(String fieldName, long value) {
		Preconditions.checkNotNull(fieldName);
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

	public static byte[] toByteArray(JsonNode node) {
		var out = new ByteArrayOutputStream();
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

	public static JsonNode read(byte[] in) {
		try {
			return MAPPER.readTree(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("Can't read json: '" + new String(in, StandardCharsets.UTF_8) + "'");
		}
	}

	public static ObjectNode readObject(String in) {
		return (ObjectNode) read(in);
	}

	public static ArrayNode readArray(String in) {
		return (ArrayNode) read(in);
	}

	public static JsonNode read(String in) {
		if (Strings.isNullOrEmpty(in)) {
			return MissingNode.getInstance();
		}
		try {
			return MAPPER.readTree(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("Can't read json: '" + in + "' [" + in.length() + "]");
		}
	}

	/**
	 * Pretty-prints a node.
	 */
	public static String toString(JsonNode node) {
		var mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		var s = new StringWriter();
		try {
			mapper.writeValue(s, node);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return s.toString();
	}
}
