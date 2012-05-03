package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class ListWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		Identity principal = new Identity();

		e1 = new Event();
		e1.setValue(Event.AUTHOR, principal);
		e1.setValue(Event.TAG, "alpha");
		e1.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC).minusHours(1));

		e2 = new Event();
		e2.setValue(Event.AUTHOR, principal);
		e2.setValue(Event.TAG, "gamma");
		e2.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC).minusHours(2));

		e3 = new Event();
		e3.setValue(Event.AUTHOR, principal);
		e3.setValue(Event.TAG, "beta");
		e3.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC).minusHours(3));
	}

	@Test
	public void testDefault() {

		addEvent(e2);
		addEvent(e1);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s", id, ListWidget.TYPE));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		assertThat(result).path(id).hasSize(3);
		assertThat(result).path(id).path(0).isEqualTo(e1.toJson());
		assertThat(result).path(id).path(1).isEqualTo(e2.toJson());
		assertThat(result).path(id).path(2).isEqualTo(e3.toJson());
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s", id, ListWidget.TYPE));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}

	@Test
	public void testConfigured() {

		addEvent(e2);
		addEvent(e1);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,offset:%d,limit:%d,order:%s,reverse:%s", id, ListWidget.TYPE, 1, 1, Event.TAG.getName(), true));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		assertThat(result).path(id).hasSize(1);
		assertThat(result).path(id).path(0).isEqualTo(e3.toJson());
	}
}
