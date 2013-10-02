package com.zenobase.io;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

public class OpenGraphTest {

	private String url = "http://zenobase.com/";

	@Test
	public void testDocumentTitle() throws IOException {
		ObjectNode node = parse("<html><title>Test</title></html>");
		assertThat(node).path("url").isEqualTo(url);
		assertThat(node).path("title").isEqualTo("Test");
	}

	@Test
	public void testMetaTitle() throws IOException {
		ObjectNode node = parse("<html><meta name='title' content='Test'></html>");
		assertThat(node).path("url").isEqualTo(url);
		assertThat(node).path("title").isEqualTo("Test");
	}

	@Test
	public void testOpenGraphTitle() throws IOException {
		ObjectNode node = parse("<html><meta property='og:title' content='Test'></html>");
		assertThat(node).path("url").isEqualTo(url);
		assertThat(node).path("title").isEqualTo("Test");
	}

	@Test
	public void testMissingTitle() throws IOException {
		ObjectNode node = parse("<html></html>");
		assertThat(node).path("url").isEqualTo(url);
		assertThat(node).path("title").isMissingNode();
	}

	private ObjectNode parse(String html) throws IOException {
		return OpenGraph.parse(url, new ByteArrayInputStream(html.getBytes())).toJson();
	}
}
