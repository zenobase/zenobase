package com.zenobase.tasks.moodpanda;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.libs.XPath;
import com.google.common.base.Strings;

class XmlResultSupport {

	private final Document document;

	protected XmlResultSupport(Document document) {
		this.document = document;
	}

	protected NodeList selectNodes(String path) {
		return XPath.selectNodes(path, document);
	}

	protected String selectText(String path) {
		return selectText(path, document);
	}

	protected String selectText(String path, Object node) {
		return Strings.emptyToNull(XPath.selectText(path, node));
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
