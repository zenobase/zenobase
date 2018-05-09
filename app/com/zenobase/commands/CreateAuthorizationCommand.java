package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class CreateAuthorizationCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create authorization", 1);
	private static final ObjectField AUTHORIZATION = new ObjectField("authorization");

	private CreateAuthorizationCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateAuthorizationCommand(Identity principal, Authorization authorization) {
		super(TYPE, principal, authorization.getCreated());
		setParameter(AUTHORIZATION, authorization.toJson());
	}

	public Authorization getAuthorization() {
		return new Authorization(getParameter(AUTHORIZATION));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteAuthorizationCommand(principal, getAuthorization());
	}

	@Override
	public String toString() {
		return "created an authorization";
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new CreateAuthorizationCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<CreateAuthorizationCommand> {

		private final AuthorizationRepository repository;

		@Inject
		public Handler(AuthorizationRepository repository) {
			super(CreateAuthorizationCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateAuthorizationCommand command) {
			repository.store(command.getAuthorization(), command.getTimestamp());
		}
	}
}
