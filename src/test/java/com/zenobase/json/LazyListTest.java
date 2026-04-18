package com.zenobase.json;

import static com.zenobase.testing.PartialListAssert.assertThat;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;

public class LazyListTest {

	@Test
	public void test() {
		PartialList<Thneed> expected = DefaultPartialList.of(List.of(new Thneed("alpha"), new Thneed("beta")), 42L);
		LazyList<Thneed> actual = new TestableList(
			new NodeList(Iterables.transform(expected, TO_JSON), expected.getTotal())
		);
		assertThat(actual).hasTotal(expected.getTotal()).isEqualTo((List<?>) expected);
	}

	private static Function<DomainNode, ObjectNode> TO_JSON = DomainNode::toJson;

	private static class TestableList extends LazyList<Thneed> {

		public TestableList(PartialList<ObjectNode> nodes) {
			super(nodes);
		}

		@Override
		protected Thneed toObject(ObjectNode node) {
			return new Thneed(node);
		}
	}

	private static class Thneed extends DomainNode {

		private static final TokenField LABEL = new TokenField("label");

		public Thneed(String label) {
			setValue(LABEL, label);
		}

		public Thneed(ObjectNode node) {
			super(node);
		}
	}
}
