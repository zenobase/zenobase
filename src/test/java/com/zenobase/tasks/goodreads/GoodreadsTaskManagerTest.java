package com.zenobase.tasks.goodreads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class GoodreadsTaskManagerTest {

	@Test
	public void parseXml() {
		assertThat(GoodreadsTaskManager.parseXml("<a><b>c</b></a>").getDocumentElement().getTagName()).isEqualTo("a");
	}

	@Test
	public void parseXmlRejectsDoctype() {
		String xml = "<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>";
		assertThatThrownBy(() -> GoodreadsTaskManager.parseXml(xml)).isInstanceOf(RuntimeException.class);
	}
}
