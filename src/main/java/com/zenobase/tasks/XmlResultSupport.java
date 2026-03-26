package com.zenobase.tasks;

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.google.common.base.Strings;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlResultSupport {

	private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

	private final Document document;

	protected XmlResultSupport(Document document) {
		this.document = document;
	}

	protected NodeList selectNodes(String path) {
		try {
			XPath xpath = XPATH_FACTORY.newXPath();
			return (NodeList) xpath.evaluate(path, document, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	protected @Nullable String selectText(String path) {
		return selectText(path, document);
	}

	protected @Nullable String selectText(String path, Object node) {
		try {
			XPath xpath = XPATH_FACTORY.newXPath();
			String text = xpath.evaluate(path, node);
			return Strings.emptyToNull(text);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	protected static String toString(Node node) {
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter sw = new StringWriter();
			t.transform(new DOMSource(node), new StreamResult(sw));
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
