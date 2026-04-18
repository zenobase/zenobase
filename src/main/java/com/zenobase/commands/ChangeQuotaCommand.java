package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class ChangeQuotaCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("change quota", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final IntegerField FROM = new IntegerField("from");
	private static final IntegerField TO = new IntegerField("to");

	private ChangeQuotaCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public ChangeQuotaCommand(
		Identity principal,
		@Nullable String username,
		@Nullable Integer from,
		@Nullable Integer to
	) {
		super(TYPE, principal);
		setParameter(USERNAME, username);
		setParameter(FROM, from);
		setParameter(TO, to);
	}

	private @Nullable String getUsername() {
		return getParameter(USERNAME);
	}

	private @Nullable Integer getFrom() {
		return getParameter(FROM);
	}

	private @Nullable Integer getTo() {
		return getParameter(TO);
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangeQuotaCommand(principal, getUsername(), getTo(), getFrom());
	}

	@Override
	public String toString() {
		return String.format("changed quota for %s to %d", getUsername(), getTo());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new ChangeQuotaCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<ChangeQuotaCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(ChangeQuotaCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(ChangeQuotaCommand command) {
			User user = repository.find(Objects.requireNonNull(command.getUsername()));
			if (user != null) {
				Preconditions.checkState(
					Objects.equals(command.getFrom(), user.getQuota()),
					"Conflict: Expected <%s> but got <%s>",
					command.getFrom(),
					user.getQuota()
				);
				user.setQuota(command.getTo());
				repository.update(user);
			} else {
				throw new NonExistentUserException(
					"Tried to change the quota of a nonexistent user: " + command.getUsername()
				);
			}
		}
	}
}
