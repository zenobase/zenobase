package com.zenobase.json;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonStream {

	private final JsonGenerator generator;

	public JsonStream(OutputStream out) throws IOException {
		generator = new JsonFactory().createJsonGenerator(out);
		generator.setCodec(new ObjectMapper());
		generator.useDefaultPrettyPrinter();
		generator.writeStartObject();
	}

	public void write(String fieldName, Integer value) throws IOException {
		if (value != null) {
			generator.writeNumberField(fieldName, value);
		}
	}

	public void write(String fieldName, String[] values) throws IOException {
		if (values != null && values.length > 0) {
			generator.writeArrayFieldStart(fieldName);
			for (String value : values) {
				generator.writeString(value);
			}
			generator.writeEndArray();
		}

	}

	public void write(JsonNode node) throws IOException {
		generator.writeTree(node);
	}

	public void writeArrayFieldStart(String fieldName) throws IOException {
		generator.writeArrayFieldStart(fieldName);
	}

	public void writeEndArray() throws IOException {
		generator.writeEndArray();
	}

	public void close() throws IOException {
		generator.writeEndObject();
		generator.close();
	}
}
