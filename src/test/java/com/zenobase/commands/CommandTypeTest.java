package com.zenobase.commands;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class CommandTypeTest {

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
				.addEqualityGroup(new Command.Type("foo", 1), new Command.Type("foo", 1))
				.addEqualityGroup(new Command.Type("foo", 2))
				.addEqualityGroup(new Command.Type("bar", 1))
				.testEquals();
	}
}
