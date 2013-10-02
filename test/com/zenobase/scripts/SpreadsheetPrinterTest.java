package com.zenobase.scripts;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import com.zenobase.json.Nodes;
import com.zenobase.scripts.SpreadsheetPrinter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import com.google.common.collect.Lists;

public class SpreadsheetPrinterTest {

	@Test
	public void test() throws IOException {

		ArrayNode node = Nodes.newArray();

		ObjectNode n1 = Nodes.newObject();
		n1.put("label", "Foo");
		n1.put("count", 42);
		node.add(n1);

		ObjectNode n2 = Nodes.newObject();
		n2.put("label", "Bar");
		n2.put("color", Nodes.newArray(Lists.newArrayList("red", "green", "blue")));
		node.add(n2);

		ObjectNode n3 = Nodes.newObject();
		n3.put("label", "Baz");
		ObjectNode nested = Nodes.newObject();
		nested.put("@value", 100);
		nested.put("unit", "mi");
		n3.put("nested", nested);
		node.add(n3);

		StringWriter out = new StringWriter();
		SpreadsheetPrinter printer = new SpreadsheetPrinter(out);
		printer.print(node);
		printer.close();

		assertThat(out.toString()).isEqualTo(
			"\"label\",\"count\",\"color\",\"nested.@value\",\"nested.unit\"\n" +
			"\"Foo\",\"42\",\"\",\"\",\"\"\n" +
			"\"Bar\",\"\",\"red,green,blue\",\"\",\"\"\n" +
			"\"Baz\",\"\",\"\",\"100\",\"mi\"\n"
		);
	}
}
