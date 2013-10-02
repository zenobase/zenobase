package com.zenobase.io;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.zenobase.json.Nodes;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;

public class OpenGraph {

	private static final TokenField URL = new TokenField("url");
	private static final TextField TITLE = new TextField("title");

	private final String url;
	private final Document doc;

	public OpenGraph(String url, Document doc) {
		this.url = url;
		this.doc = doc;
	}

	public String getUrl() {
		return url;
	}

	public String getTitle() {
		String title = getOpenGraphTitle();
		if (title == null) {
			title = getMetaTitle();
		}
		if (title == null) {
			title = getDocumentTitle();
		}
		return title;
	}

	private String getOpenGraphTitle() {
		Element element = doc.select("meta[property=og:title]").first();
		return element != null ? element.attr("content") : null;
	}

	private String getMetaTitle() {
		Element element = doc.select("meta[name=title]").first();
		return element != null ? element.attr("content") : null;
	}

	private String getDocumentTitle() {
		Element element = doc.select("title").first();
		return element != null ? element.text() : null;
	}

	public ObjectNode toJson() {
		ObjectNode node = Nodes.newObject();
		TITLE.setValue(node, getTitle());
		URL.setValue(node, getUrl());
		return node;
	}

	public static OpenGraph parse(String url, InputStream in) throws IOException {
		return new OpenGraph(url, Jsoup.parse(in, null, url));
	}
}
