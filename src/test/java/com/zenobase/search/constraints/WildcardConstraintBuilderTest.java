package com.zenobase.search.constraints;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.models.Event;
import com.zenobase.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WildcardConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@BeforeEach
	public void addEvents() {
		addEvent("lunch");
		addEvent("lurch");
		addEvent("bunch");
	}

	private void addEvent(String tag) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		addEvent(event);
	}

	@Test
	public void testPrefix() {
		addConstraint("%s:%s", Event.TAG, "lu*");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testPostfix() {
		addConstraint("%s:%s", Event.TAG, "*nch");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testPrefixAndPostfix() {
		addConstraint("%s:%s", Event.TAG, "lu*ch");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testEscaped() {
		addConstraint("%s:%s", Event.TAG, "lu\\*");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}
}
