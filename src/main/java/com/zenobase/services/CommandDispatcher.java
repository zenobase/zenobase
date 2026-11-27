package com.zenobase.services;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.repositories.CommandRepository;
import io.sentry.Sentry;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandDispatcher {

	private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

	private static final List<Duration> RETRY_DELAYS = List.of(Duration.ofMillis(200), Duration.ofSeconds(1));

	private final DateTime now = DateTime.now(DateTimeZone.UTC).minusSeconds(1);
	private final CommandHandlerRegistry handlers;
	private final CommandRepository repository;
	private final QuotaManager quotas;
	private final List<Duration> retryDelays;

	@Inject
	public CommandDispatcher(CommandHandlerRegistry handlers, CommandRepository repository, QuotaManager quotas) {
		this(handlers, repository, quotas, RETRY_DELAYS);
	}

	CommandDispatcher(
		CommandHandlerRegistry handlers,
		CommandRepository repository,
		QuotaManager quotas,
		List<Duration> retryDelays
	) {
		this.handlers = handlers;
		this.repository = repository;
		this.quotas = quotas;
		this.retryDelays = retryDelays;
	}

	public String dispatch(Command command) {
		DateTime t = command.getTimestamp();
		logger.info("{} [{}] {} {}", command.getId(), t, command.getPrincipal(), command);
		if (command.getCost() > 0 && t.isAfter(now)) {
			// don't spend while replaying commands
			quotas.spend(command.getPrincipal(), command.getCost());
		}
		apply(command);
		journal(command);
		return command.getId();
	}

	private void apply(Command command) {
		if (command instanceof CompoundCommand c) {
			dispatch(c);
		} else {
			handlers.execute(command);
		}
	}

	/**
	 * Appends an applied command to the journal, retrying transient failures. If it can't be journaled, reverts the
	 * command so that state matches the journal, and rethrows.
	 */
	private void journal(Command command) {
		RuntimeException failure;
		try {
			repository.put(command);
			return;
		} catch (RuntimeException e) {
			if (isConflict(e)) {
				throw e; // already journaled, e.g. a duplicate during replay
			}
			failure = e;
		}
		for (Duration delay : retryDelays) {
			logger.warn("Retrying journal write for {}: {}", command.getId(), failure.getMessage());
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			try {
				repository.put(command);
				return;
			} catch (RuntimeException e) {
				if (isConflict(e)) {
					return; // an earlier attempt went through
				}
				failure = e;
			}
		}
		revert(command, failure);
		throw failure;
	}

	private void revert(Command command, RuntimeException failure) {
		logger.error("Couldn't journal {}, reverting: {} {}", command.getId(), command.getPrincipal(), command);
		try {
			apply(command.reverse(command.getPrincipal()));
		} catch (RuntimeException e) {
			failure.addSuppressed(e);
			logger.error(
				"Couldn't revert unjournaled command {} ({})",
				command.getId(),
				command.getType().name(),
				failure
			);
			Sentry.captureException(failure, scope -> {
				scope.setTag("command.id", command.getId());
				scope.setTag("command.type", command.getType().name());
			});
		}
	}

	private static boolean isConflict(RuntimeException e) {
		return e instanceof OpenSearchException ose && ose.status() == 409;
	}

	private void dispatch(CompoundCommand command) {
		List<Command> dispatched = new ArrayList<>();
		try {
			for (Command c : command.getCommands()) {
				handlers.execute(c);
				dispatched.add(c);
			}
		} catch (RuntimeException e) {
			if (!dispatched.isEmpty()) {
				int count = dispatched.size();
				logger.warn("Reverting {} {} ({})...", command.getPrincipal(), command, count);
				try {
					while (count-- > 0) {
						Command c = dispatched.get(count);
						handlers.execute(c.reverse(c.getPrincipal()));
					}
				} catch (RuntimeException e2) {
					logger.error(
						"Couldn't revert {} {} ({}/{})...",
						command.getPrincipal(),
						command,
						count + 1,
						dispatched.size()
					);
					throw e;
				}
			}
			throw e;
		}
	}

	public void discard(Command command) {
		logger.info("[{} {}]", command.getPrincipal(), command);
	}
}
