package com.zenobase.json;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonStream {

	private final JsonGenerator generator;

	public JsonStream(OutputStream out) throws IOException {
		generator = new JsonFactory().createGenerator(out);
		generator.setCodec(new ObjectMapper());
		generator.useDefaultPrettyPrinter();
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
