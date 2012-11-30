package com.zenobase.tasks;

import java.io.IOException;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.io.ByteStreams;

import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public class ResultTestSupport {

	protected static final Identity TESTER = new Identity();

	protected ObjectNode readObject(String filename) {
		try {
			return Nodes.readObject(readBytes(filename));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	protected ArrayNode readArray(String filename) {
		try {
			return Nodes.readArray(readBytes(filename));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private byte[] readBytes(String filename) throws IOException {
		return ByteStreams.toByteArray(getClass().getResourceAsStream(filename));
	}
}
