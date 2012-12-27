package com.zenobase.search;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class EventSearchTest {

	@Test(expected = IllegalArgumentException.class)
	public void testUnsupportedFilter() {
		new EventSearchBuilder().addConstraint("xxx:lunch");
	}

	@Test
	public void testUnsupportedWidget() {
		new EventSearchBuilder().addWidget("type:xxx");
	}

	@Test
	public void testEqualsHashCode() {
		Search a = new EventSearchBuilder().addConstraint("tag:a").addConstraint("tag:b").build();
		Search b = new EventSearchBuilder().addConstraints(new String[] { "tag:a", "tag:b" }).build();
		Search c = new EventSearchBuilder().addWidget("id:c1,type:list").addWidget("id:c2,type:count").build();
		Search d = new EventSearchBuilder().addWidgets(new String[] { "id:c1,type:list", "id:c2,type:count" }).build();
		Search e = new EventSearchBuilder().build();
		Search f = new EventSearchBuilder().addConstraints(null).addWidgets(null).build();
		Search g = new EventSearchBuilder().addConstraint("tag:a").addWidget("id:c2,type:count").build();
		new EqualsTester()
			.addEqualityGroup(a, b)
			.addEqualityGroup(c, d)
			.addEqualityGroup(e, f)
			.addEqualityGroup(g)
			.testEquals();
	}
}
