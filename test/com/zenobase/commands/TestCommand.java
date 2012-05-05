package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class TestCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("test command", 1);
	private static final TokenField TAG = new TokenField("tag");

	public TestCommand(Identity principal, String tag) {
		super(TYPE, principal);
		setParameter(TAG, tag);
	}

	public TestCommand(ObjectNode node) {
		super(node);
	}

	public String getTag() {
		return getParameter(TAG);
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
