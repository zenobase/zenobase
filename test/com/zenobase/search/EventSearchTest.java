package com.zenobase.search;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class EventSearchTest {

	@Test
	public void testEqualsHashCode() {
		EventSearch a = new EventSearch().addFilter("tag:a").addFilter("tag:b");
		EventSearch b = new EventSearch().addFilters(new String[] { "tag:a", "tag:b" });
		EventSearch c = new EventSearch().addWidget("id:c1,type:list").addWidget("id:c2,type:count");
		EventSearch d = new EventSearch().addWidgets(new String[] { "id:c1,type:list", "id:c2,type:count" });
		EventSearch e = new EventSearch();
		EventSearch f = new EventSearch().addFilters(null).addWidgets(null);
		EventSearch g = new EventSearch().addFilter("tag:a").addWidget("id:c2,type:count");
		new EqualsTester()
			.addEqualityGroup(a, b)
			.addEqualityGroup(c, d)
			.addEqualityGroup(e, f)
			.addEqualityGroup(g)
			.testEquals();
	}
}
