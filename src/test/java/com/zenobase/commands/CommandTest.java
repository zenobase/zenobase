package com.zenobase.commands;

import com.google.common.testing.EqualsTester;
import com.zenobase.models.Identity;
import org.junit.jupiter.api.Test;

public class CommandTest {

	@Test
	public void testEqualsHashCode() {
		Identity principal = new Identity();
		Command command = new TestCommand(principal, "foo");
		new EqualsTester()
			.addEqualityGroup(command, new TestCommand(command.toJson()))
			.addEqualityGroup(new TestCommand(principal, "foo"))
			.testEquals();
	}
}
