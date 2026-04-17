package com.zenobase.search.constraints;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.json.ResourceField;
import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.search.Search;

public class ResourceConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@BeforeEach
	public void addEvents() {
		addEvent(Event.RESOURCE, "Zenobase", "https://zenobase.com/");
		addEvent(Event.RESOURCE, "Zenobase Blog", "http://blog.zenobase.com/");
		addEvent(Event.RESOURCE, "Quantified Self", "http://quantifiedself.com/");
		addEvent(Event.SOURCE, "Test", "http://test/");
	}

	private void addEvent(ResourceField field, String title, String url) {
		Event event = new Event();
		event.setValue(field, new Resource(title, url));
		addEvent(event);
	}

	@Test
	public void testExists() {
		addConstraint("resource:*");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testSearchTitleWithPrefix() {
		addConstraint("resource.title:%s", "zeno*");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testSearchTitleWithTerm() {
		addConstraint("resource.title:%s", "zenobase");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testSearchTitleWithMissingTerm() {
		addConstraint("resource.title:%s", "test");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testSearchSourceTitleWithTerm() {
		addConstraint("source.title:%s", "test");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testSearchTitleWithPhrase() {
		addConstraint("resource.title:%s", "\"zenobase blog\"");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testSearchUrlWithPrefix() {
		addConstraint("resource.url:%s", "http:*");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testSearchUrlWithTerm() {
		addConstraint("resource.url:%s", "zenobase");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testUrlEquals() {
		addConstraint("resource.url:%s", "https://zenobase.com/");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}
}
