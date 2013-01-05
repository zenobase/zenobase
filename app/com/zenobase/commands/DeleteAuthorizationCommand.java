package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class DeleteAuthorizationCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete authorization", 1);
	private static final ObjectField AUTHORIZATION = new ObjectField("authorization");

	private DeleteAuthorizationCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteAuthorizationCommand(Identity principal, Authorization authorization) {
		super(TYPE, principal);
		setParameter(AUTHORIZATION, authorization.toJson());
	}

	private Authorization getAuthorization() {
		return new Authorization(getParameter(AUTHORIZATION));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateAuthorizationCommand(principal, getAuthorization());
	}

	@Override
	public String toString() {
		return String.format("removed an authorization");
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new DeleteAuthorizationCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<DeleteAuthorizationCommand> {

		private final AuthorizationRepository repository;

		@Inject
		public Handler(AuthorizationRepository repository) {
			super(DeleteAuthorizationCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteAuthorizationCommand command) {
			repository.delete(command.getAuthorization().getId());
		}
	}
}
