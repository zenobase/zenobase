package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;

public class ChangeExternalIdCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("change external id", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField EXTERNAL_ID = new TokenField("external_id");

	private ChangeExternalIdCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public ChangeExternalIdCommand(Identity principal, String name, String externalId) {
		super(TYPE, principal);
		setParameter(USERNAME, name);
		setParameter(EXTERNAL_ID, externalId);
	}

	private String getName() {
		return Objects.requireNonNull(getParameter(USERNAME));
	}

	private String getExternalId() {
		return Objects.requireNonNull(getParameter(EXTERNAL_ID));
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangeExternalIdCommand(principal, getName(), "");
	}

	@Override
	public String toString() {
		return String.format("set external id for %s", getName());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new ChangeExternalIdCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<ChangeExternalIdCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(ChangeExternalIdCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(ChangeExternalIdCommand command) {
			User user = Objects.requireNonNull(repository.find(command.getName()));
			user.setExternalId(command.getExternalId());
			repository.update(user);
		}
	}
}
