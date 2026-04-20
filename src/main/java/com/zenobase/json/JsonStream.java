package com.zenobase.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.OutputStream;

public class JsonStream {

	private static final ObjectMapper MAPPER = new ObjectMapper().disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);

	private final JsonGenerator generator;

	public JsonStream(OutputStream out) throws IOException {
		generator = new JsonFactory().createGenerator(out);
		generator.setCodec(MAPPER);
		generator.writeStartObject();
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
