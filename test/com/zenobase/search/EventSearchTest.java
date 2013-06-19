package com.zenobase.search;

import org.elasticsearch.common.collect.Lists;
import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class EventSearchTest {

	@Test(expected = IllegalArgumentException.class)
	public void testUnsupportedFilter() {
		new EventSearchBuilder().addConstraint("xxx:lunch");
	}

	@Test
	public void testUnsupportedFacet() {
		new EventSearchBuilder().addFacet("type:xxx");
	}

	@Test
	public void testEqualsHashCode() {
		Search a = new EventSearchBuilder().addConstraint("tag:a").addConstraint("tag:b").build();
		Search b = new EventSearchBuilder().addConstraints(Lists.newArrayList("tag:a", "tag:b")).build();
		Search c = new EventSearchBuilder().addFacet("id:c1,type:list").addFacet("id:c2,type:count").build();
		Search d = new EventSearchBuilder().addFacets(new String[] { "id:c1,type:list", "id:c2,type:count" }).build();
		Search e = new EventSearchBuilder().build();
		Search f = new EventSearchBuilder().addConstraints(Lists.<String>newArrayList()).addFacets(new String[0]).build();
		Search g = new EventSearchBuilder().addConstraint("tag:a").addFacet("id:c2,type:count").build();
		new EqualsTester()
			.addEqualityGroup(a, b)
			.addEqualityGroup(c, d)
			.addEqualityGroup(e, f)
			.addEqualityGroup(g)
			.testEquals();
	}
}
