package com.zenobase.json;

import org.junit.Test;

import com.zenobase.commands.Command;

public class CommandTypeTest extends FieldTestSupport<Command.Type> {

	@Override
	protected Field<Command.Type> newField(String name) {
		return new CommandTypeField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(new Command.Type("do something", 1));
	}
}
