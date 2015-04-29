package com.zenobase.tasks;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

	protected Document readXml(String filename) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(openStream(filename));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private byte[] readBytes(String filename) throws IOException {
		return ByteStreams.toByteArray(openStream(filename));
	}

	private InputStream openStream(String filename) {
		return getClass().getResourceAsStream(filename);
	}

	protected static DateTime dateTime(String value) {
		return DateTime.parse(value, ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed());
	}
}
