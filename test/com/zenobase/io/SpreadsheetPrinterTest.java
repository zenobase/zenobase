package com.zenobase.io;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;

public class SpreadsheetPrinterTest {

	@Test
	public void test() throws IOException {

		ArrayNode node = Nodes.newArray();

		ObjectNode n1 = Nodes.newObject();
		n1.put("label", "Foo");
		n1.put("value", "1\"");
		node.add(n1);

		ObjectNode n2 = Nodes.newObject();
		n2.put("label", "Bar");
		n2.put("values", Nodes.newArray().add("red").add("green").add("blue"));
		node.add(n2);

		ObjectNode n3 = Nodes.newObject();
		n3.put("label", "Baz");
		n3.put("object", Nodes.newObject().put("@value", 100).put("unit", "mi"));
		node.add(n3);

		ObjectNode n4 = Nodes.newObject();
		n4.put("label", "Qux");
		n4.put("objects", Nodes.newArray()
			.add(Nodes.newObject().put("lat", 1).put("lon", 2))
			.add(Nodes.newObject().put("lat", 3).put("lon", 4)));
		node.add(n4);

		StringWriter out = new StringWriter();
		SpreadsheetPrinter printer = new SpreadsheetPrinter(out);
		printer.print(node);
		printer.close();

		assertThat(out.toString()).isEqualTo(
			"\"label\",\"value\",\"values\",\"object.@value\",\"object.unit\",\"objects.lat\",\"objects.lon\"\n" +
			"\"Foo\",\"1\"\"\",\"\",\"\",\"\",\"\",\"\"\n" +
			"\"Bar\",\"\",\"red;green;blue\",\"\",\"\",\"\",\"\"\n" +
			"\"Baz\",\"\",\"\",\"100\",\"mi\",\"\",\"\"\n" +
			"\"Qux\",\"\",\"\",\"\",\"\",\"1;3\",\"2;4\"\n"
		);
	}
}
