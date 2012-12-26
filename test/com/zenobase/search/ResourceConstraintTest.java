package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Resource;

public class ResourceConstraintTest extends ConstraintTestSupport {

	@Before
	public void addEvents() {
		addEvent("Zenobase", "https://zenobase.com/");
		addEvent("Zenobase Blog", "http://blog.zenobase.com/");
		addEvent("Quantified Self", "http://quantifiedself.com/");
	}

	private void addEvent(String title, String url) {
		Event event = new Event();
		event.setValue(Event.RESOURCE, new Resource(title, url));
		addEvent(event);
	}

	@Test
	public void testSearchTitleWithPrefix() {
		addConstraint("resource.title:%s", "zeno*");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testSearchTitleWithTerm() {
		addConstraint("resource.title:%s", "zenobase");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testSearchTitleWithPhrase() {
		addConstraint("resource.title:%s", "\"zenobase blog\"");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testSearchUrlWithPrefix() {
		addConstraint("resource.url:%s", "http:*");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testSearchUrlWithTerm() {
		addConstraint("resource.url:%s", "zenobase");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testUrlEquals() {
		addConstraint("resource.url:%s", "https://zenobase.com/");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}
}
