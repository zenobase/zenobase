package com.zenobase.commands;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

import com.zenobase.models.Identity;

public class CommandTest {

	@Test
	public void testEqualsHashCode() {
		Identity principal = new Identity();
		Command command = new TestCommand(principal, "foo");
		new EqualsTester()
			.addEqualityGroup(command, new TestCommand(command.toJson()))
			.addEqualityGroup(new TestCommand(principal, "foo")).testEquals();
	}
}
