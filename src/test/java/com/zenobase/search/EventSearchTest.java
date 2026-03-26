package com.zenobase.search;

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

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
		Search a = new EventSearchBuilder()
				.addConstraint("tag:a")
				.addConstraint("tag:b")
				.buildSearch();
		Search b = new EventSearchBuilder()
				.addConstraints(Lists.newArrayList("tag:a", "tag:b"))
				.buildSearch();
		Search c = new EventSearchBuilder()
				.addFacet("id:c1,type:list")
				.addFacet("id:c2,type:count")
				.buildSearch();
		Search d = new EventSearchBuilder()
				.addFacets(new String[] {"id:c1,type:list", "id:c2,type:count"})
				.buildSearch();
		Search e = new EventSearchBuilder().buildSearch();
		Search f = new EventSearchBuilder()
				.addConstraints(Lists.newArrayList())
				.addFacets(new String[0])
				.buildSearch();
		Search g = new EventSearchBuilder()
				.addConstraint("tag:a")
				.addFacet("id:c2,type:count")
				.buildSearch();
		new EqualsTester()
				.addEqualityGroup(a, b)
				.addEqualityGroup(c, d)
				.addEqualityGroup(e, f)
				.addEqualityGroup(g)
				.testEquals();
	}
}
