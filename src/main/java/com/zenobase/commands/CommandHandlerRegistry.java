package com.zenobase.commands;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class CommandHandlerRegistry {

	private final Map<Class<? extends Command>, CommandHandler<?>> handlers = Maps.newHashMap();

	@Inject
	public CommandHandlerRegistry(Set<CommandHandler<?>> handlers) {
		for (CommandHandler<?> handler : handlers) {
			this.handlers.put(handler.getType(), handler);
		}
	}

	public static CommandHandlerRegistry containing(CommandHandler<?>... handlers) {
		return new CommandHandlerRegistry(ImmutableSet.copyOf(handlers));
	}

	public void execute(Command command) {
		CommandHandler<?> handler = handlers.get(command.getClass());
		Preconditions.checkNotNull(handler, "Missing handler for %s", command.getClass());
		handler.execute(command);
	}
}
