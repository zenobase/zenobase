package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class EmptyWidgetTest extends WidgetTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		addEvent(new Event());
		addEvent(new Event());
		addEvent(new Event());
	}

	@Test
	public void testBasic() {
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
	}
}
