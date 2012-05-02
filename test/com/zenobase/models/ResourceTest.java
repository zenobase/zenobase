package com.zenobase.models;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class ResourceTest {

	@Test
	public void testEqualsHashCode() {
		Resource r1 = new Resource("Some Like It Hot", "http://www.imdb.com/title/tt0053291/");
		Resource r2 = new Resource("Some Like It Hot", "http://www.imdb.com/title/tt0110912/");
		Resource r3 = new Resource("Pulp Fiction", "http://www.imdb.com/title/tt0053291/");
		Resource r4 = new Resource("Pulp Fiction", "http://www.imdb.com/title/tt0110912/");
		new EqualsTester()
			.addEqualityGroup(r1, new Resource(r1.getTitle(), r1.getUrl()))
			.addEqualityGroup(r2).addEqualityGroup(r3)
			.addEqualityGroup(r4).testEquals();
	}
}
