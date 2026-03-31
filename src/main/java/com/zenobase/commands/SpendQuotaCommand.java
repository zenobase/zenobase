package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;

public class SpendQuotaCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("spend quota", 1);

	private SpendQuotaCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public SpendQuotaCommand(Identity principal, int cost) {
		super(TYPE, principal);
		addCost(cost);
	}

	@Override
	public Command reverse(Identity principal) {
		return new SpendQuotaCommand(principal, getCost());
	}

	@Override
	public String toString() {
		return "spent quota";
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new SpendQuotaCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<SpendQuotaCommand> {

		@Inject
		public Handler() {
			super(SpendQuotaCommand.class);
		}

		@Override
		public void executeTyped(SpendQuotaCommand command) {
			// nothing to do here!
		}
	}
}
