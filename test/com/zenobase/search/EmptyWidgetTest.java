package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.zenobase.commands.RandomEvent;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;

public class EmptyWidgetTest extends SearchTestSupport {

	@Test
	public void test() {

		RandomEvent rand = new RandomEvent(Generator.id(), new Identity());
		int size = 100;
		for (int i = 0; i < size; ++i) {
			addEvent(rand.next());
		}

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(size);
	}
}
