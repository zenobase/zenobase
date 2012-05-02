package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.test.NodeAssert;

public class CountWidgetTest extends WidgetTestSupport {

	private static final String TAG_LUNCH = "lunch";
	private static final String TAG_DINNER = "dinner";

	private String id = Generator.id();
	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		e1 = new Event();
		e1.setValue(Event.TAG, TAG_LUNCH);

		e2 = new Event();
		e2.setValue(Event.TAG, TAG_DINNER);

		e3 = new Event();
		e3.setValue(Event.TAG, TAG_LUNCH);
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s", id, "count", Event.TAG));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(TAG_DINNER);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOrderByTerm() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s,order:%s", id, "count", Event.TAG, "term"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo(TAG_LUNCH);
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testOrderByCountReverse() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s,reverse:%s", id, "count", Event.TAG, true));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo(TAG_LUNCH);
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testLimit() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s,limit:%s", id, "count", Event.TAG, 1));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo("...");
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOffset() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s,offset:%s", id, "count", Event.TAG, 1));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(1);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s,field:%s", id, "count", Event.TAG));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}
}
