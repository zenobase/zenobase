package com.zenobase.commands;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class CommandTypeTest {

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
			.addEqualityGroup(new Command.Type("foo", 1), new Command.Type("foo", 1))
			.addEqualityGroup(new Command.Type("foo", 2))
			.addEqualityGroup(new Command.Type("bar", 1)).testEquals();
	}
}
