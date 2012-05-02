package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.zenobase.models.Event;

public class EmptyWidgetTest extends WidgetTestSupport {

	@Test
	public void testBasic() {

		addEvent(new Event());
		addEvent(new Event());
		addEvent(new Event());

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
	}
}
