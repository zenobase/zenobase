package com.zenobase.json;

import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.json.CommandTypeField;

public class CommandTypeTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new CommandTypeField(FIELD_NAME), new Command.Type("do something", 1));
	}
}
