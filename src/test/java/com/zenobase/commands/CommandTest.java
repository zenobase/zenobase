package com.zenobase.commands;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import com.zenobase.models.Identity;

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
