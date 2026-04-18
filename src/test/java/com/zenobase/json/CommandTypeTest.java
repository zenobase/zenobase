package com.zenobase.json;

import com.zenobase.commands.Command;
import org.junit.jupiter.api.Test;

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
