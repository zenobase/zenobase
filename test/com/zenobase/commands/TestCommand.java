package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class TestCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("test command", 1);
	private static final TokenField TAG = new TokenField("tag");

	public TestCommand(Identity principal, String tag) {
		super(TYPE, principal);
		setParameter(TAG, tag);
		addCost(1);
	}

	public TestCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public String getTag() {
		return getParameter(TAG);
	}

	public TestCommand setTimestamp(DateTime timestamp) {
		setValue(TIMESTAMP, timestamp);
		return this;
	}

	@Override
	public Command reverse(Identity principal) {
		return new TestCommand(principal, new StringBuilder(getTag()).reverse().toString());
	}

	@Override
	public String toString() {
		return String.format("%s: %s", TYPE.getName(), getTag());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			Preconditions.checkArgument(version == TYPE.getVersion());
			return new TestCommand(node);
		}
	}
}
