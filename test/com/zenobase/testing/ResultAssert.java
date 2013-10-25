package com.zenobase.testing;

import static play.test.Helpers.*;

import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import play.mvc.Result;
import play.test.Helpers;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;

public class ResultAssert extends GenericAssert<ResultAssert, Result> {

	private ResultAssert(Result actual) {
		super(ResultAssert.class, actual);
	}

	public static ResultAssert assertThat(Result actual) {
		return new ResultAssert(actual);
	}

	public ResultAssert hasStatus(int status) {
		Assertions.assertThat(Helpers.status(actual)).as("status of result").isEqualTo(status);
		return this;
	}

	public ResultAssert hasHeader(String name, String value) {
		Assertions.assertThat(header(name, actual)).isEqualTo(value);
		return this;
	}

	public ResultAssert hasContentType(String contentType) {
		Assertions.assertThat(Helpers.contentType(actual)).as("content type").isEqualTo(contentType);
		return this;
	}

	public ResultAssert hasContent(ObjectNode node) {
		hasContentType("application/json");
		NodeAssert.assertThat(Nodes.readObject(contentAsBytes(actual))).isEqualTo(node);
		return this;
	}

	public ResultAssert hasContent(ArrayNode node) {
		hasContentType("application/json");
		NodeAssert.assertThat(Nodes.readArray(contentAsBytes(actual))).isEqualTo(node);
		return this;
	}

	public ResultAssert hasContent(String content) {
		hasContentType("text/plain");
		Assertions.assertThat(contentAsString(actual)).isEqualTo(content);
		return this;
	}

	public NodeAssert asObjectNode() {
		hasContentType("application/json");
		return NodeAssert.assertThat(Nodes.readObject(contentAsBytes(actual)));
	}

	public NodeAssert asArrayNode() {
		hasContentType("application/json");
		return NodeAssert.assertThat(Nodes.readArray(contentAsBytes(actual)));
	}

	public ResultAssert isEmpty() {
		hasContentType(null);
		Assertions.assertThat(Helpers.contentAsBytes(actual).length).as("content length").isZero();
		return this;
	}
}
