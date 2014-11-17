package com.zenobase.search;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;

public class FacetOptionsTest {

	@Test
	public void test() {
		check(new FacetOptions(ImmutableMap.<String, String>builder()
			.put("a", "foo")
			.put("b", "42")
			.put("c", "1.23456789")
			.put("d", "true")
			.put("e", "-08:00")
			.put("l", "l1,l2,l3,l4")
			.build()));
	}

	@Test
	public void testParse() {
		check(FacetOptions.parse("a:foo,b:42,c:1.23456789,d:true,e:-08:00,f,g:,h:null,:,l:l1\\,l2\\,l3\\,l4"));
	}

	private static void check(FacetOptions options) {

		assertThat(options.get("a")).isEqualTo("foo");
		assertThat(options.get("z", String.class, "bar")).isEqualTo("bar");

		assertThat(options.get("b", Integer.class, 1)).isEqualTo(42);
		assertThat(options.get("z", Integer.class, 1)).isEqualTo(1);

		assertThat(options.get("b", Long.class, 1L)).isEqualTo(42L);
		assertThat(options.get("z", Long.class, 1L)).isEqualTo(1L);

		assertThat(options.get("c", Double.class, 1.23456789)).isEqualTo(1.23456789);
		assertThat(options.get("z", Double.class, 1.0)).isEqualTo(1.0);

		assertThat(options.get("d", Boolean.class, false)).isEqualTo(true);
		assertThat(options.get("z", Boolean.class, true)).isEqualTo(true);

		assertThat(options.get("e", DateTimeZone.class, DateTimeZone.UTC)).isEqualTo(DateTimeZone.forOffsetHours(-8));
		assertThat(options.get("z", DateTimeZone.class, DateTimeZone.UTC)).isEqualTo(DateTimeZone.UTC);

		assertThat(options.get("f", String.class, "foo")).isEqualTo("foo");
		assertThat(options.get("f", String.class, null)).isEqualTo(null);

		assertThat(options.get("g", String.class, "foo")).isEqualTo("foo");
		assertThat(options.get("g", String.class, null)).isEqualTo(null);

		assertThat(options.get("h", String.class, "foo")).isEqualTo("foo");
		assertThat(options.get("h", String.class, null)).isEqualTo(null);

		assertThat(options.get("l", String.class, null)).isEqualTo("l1,l2,l3,l4");
	}


	@Test(expected = IllegalArgumentException.class)
	public void testBadType() {
		FacetOptions.parse("a:1").get("a", FacetOptions.class, null);
	}
}
