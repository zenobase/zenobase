package com.zenobase.tasks;

import com.google.common.base.Strings;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
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
}
