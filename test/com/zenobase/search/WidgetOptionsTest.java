package com.zenobase.search;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.junit.Test;

public class WidgetOptionsTest {

	@Test
	public void test() {

		WidgetOptions options = WidgetOptions.parse("a:foo,b:42,c:1.23456789,d:true,e:-08:00,f,g:,h:null,:");

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
	}


	@Test(expected = IllegalArgumentException.class)
	public void testBadType() {
		WidgetOptions.parse("a:1").get("a", WidgetOptions.class, null);
	}
}
