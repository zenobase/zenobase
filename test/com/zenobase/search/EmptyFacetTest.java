package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import com.zenobase.commands.RandomEvent;
import com.zenobase.models.Identity;

public class EmptyFacetTest extends SearchTestSupport {

	@Test
	public void test() {

		RandomEvent rand = new RandomEvent(new Identity());
		int size = 100;
		for (int i = 0; i < size; ++i) {
			addEvent(rand.next());
		}

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(size);
	}
}
