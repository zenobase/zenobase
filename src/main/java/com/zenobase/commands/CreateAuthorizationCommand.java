package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

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
		return new Authorization(Objects.requireNonNull(getParameter(AUTHORIZATION)));
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
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new CreateAuthorizationCommand(node);
				default -> null;
			};
		}
	}
}
