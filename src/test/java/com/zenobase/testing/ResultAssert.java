package com.zenobase.testing;

import java.util.IdentityHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.assertj.core.api.Assertions;

import com.zenobase.json.Nodes;

public class ResultAssert {

	private static final Map<Http1ClientResponse, ResultAssert> CACHE = new IdentityHashMap<>();

	private final int status;
	private final byte[] body;
	private final String contentType;
	private final Http1ClientResponse response;

	private ResultAssert(Http1ClientResponse response) {
		this.response = response;
		this.status = response.status().code();
		byte[] b;
		try {
			b = response.entity().as(byte[].class);
		} catch (IllegalStateException e) {
			b = new byte[0];
		}
		this.body = b;
		this.contentType = response.headers().first(HeaderNames.CONTENT_TYPE).orElse(null);
	}

	public static ResultAssert assertThat(Http1ClientResponse response) {
		return CACHE.computeIfAbsent(response, ResultAssert::new);
	}

	public ResultAssert hasStatus(int status) {
		Assertions.assertThat(this.status).as("status of result").isEqualTo(status);
		return this;
	}

	public ResultAssert hasHeader(String name, String value) {
		Assertions.assertThat(response.headers().first(HeaderNames.create(name)).orElse(null)).isEqualTo(value);
		return this;
	}

	public ResultAssert hasContentType(String contentType) {
		if (contentType == null) {
			Assertions.assertThat(this.contentType).as("content type").isNull();
		} else {
			Assertions.assertThat(this.contentType).as("content type").contains(contentType);
		}
		return this;
	}

	public ResultAssert hasContent(ObjectNode node) {
		NodeAssert.assertThat(Nodes.readObject(body)).isEqualTo(node);
		return this;
	}

	public ResultAssert hasContent(ArrayNode node) {
		NodeAssert.assertThat(Nodes.readArray(body)).isEqualTo(node);
		return this;
	}

	public ResultAssert hasContent(String content) {
		Assertions.assertThat(new String(body)).isEqualTo(content);
		return this;
	}

	public NodeAssert asObjectNode() {
		return NodeAssert.assertThat(Nodes.readObject(body));
	}

	public NodeAssert asArrayNode() {
		return NodeAssert.assertThat(Nodes.readArray(body));
	}

	public ResultAssert isEmpty() {
		Assertions.assertThat(body.length).as("content length").isZero();
		return this;
	}
}
