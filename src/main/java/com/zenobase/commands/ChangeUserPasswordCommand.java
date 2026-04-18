package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class ChangeUserPasswordCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("change user password", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField FROM = new TokenField("from");
	private static final TokenField TO = new TokenField("to");

	private ChangeUserPasswordCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public ChangeUserPasswordCommand(Identity principal, String username, String from, String to) {
		super(TYPE, principal);
		setParameter(USERNAME, username);
		setParameter(FROM, from);
		setParameter(TO, to);
	}

	private @Nullable String getUsername() {
		return getParameter(USERNAME);
	}

	private @Nullable String getFrom() {
		return getParameter(FROM);
	}

	private @Nullable String getTo() {
		return getParameter(TO);
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangeUserPasswordCommand(
			principal,
			Objects.requireNonNull(getUsername()),
			Objects.requireNonNull(getTo()),
			Objects.requireNonNull(getFrom())
		);
	}

	@Override
	public String toString() {
		return String.format("changed password for user %s", getUsername());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new ChangeUserPasswordCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<ChangeUserPasswordCommand> {

		@Inject
		public Handler() {
			super(ChangeUserPasswordCommand.class);
		}

		@Override
		public void executeTyped(ChangeUserPasswordCommand command) {
			// No-op: passwords are now managed by Auth0
		}
	}
}
