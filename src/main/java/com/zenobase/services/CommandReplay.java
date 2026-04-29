package com.zenobase.services;

import com.google.common.base.Stopwatch;
import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.NonExistentUserException;
import com.zenobase.queries.CommandQuery;
import com.zenobase.queries.UserQuery;
import com.zenobase.repositories.CommandRepository;
import com.zenobase.repositories.IndexManager;
import com.zenobase.repositories.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandReplay {

	private static final Logger logger = LoggerFactory.getLogger(CommandReplay.class);

	private static final int MAX_FAILURES = 1;

	private final String sourceHost;
	private final CommandParserRegistry parsers;
	private final CommandDispatcher dispatcher;
	private final AtomicInteger count = new AtomicInteger();
	private final AtomicInteger replayed = new AtomicInteger();
	private final AtomicInteger failures = new AtomicInteger();

	@Inject
	public CommandReplay(
		@Named("opensearch.replay") String sourceHost,
		CommandParserRegistry parsers,
		CommandDispatcher dispatcher
	) {
		this.sourceHost = sourceHost;
		this.parsers = parsers;
		this.dispatcher = dispatcher;
	}

	public void replay() {
		if (!sourceHost.isEmpty()) {
			ClientFactory factory = () -> OpenSearchClientFactory.createClient(sourceHost, System.getenv("AWS_REGION"));
			IndexManager indexManager = new IndexManager(factory);
			replay(indexManager);
			indexManager.close();
		}
	}

	void replay(IndexManager indexManager) {
		var repository = new CommandRepository(indexManager, parsers);
		Set<String> identities = loadIdentities(indexManager);
		logger.info("Replaying {} commands from {}...", repository.size(), sourceHost);
		Stopwatch timer = Stopwatch.createStarted();
		repository.find(new CommandQuery(), SearchOrder.asc(Command.TIMESTAMP, Command.ID), command -> {
			if (failures.get() >= MAX_FAILURES) {
				throw new IllegalStateException("Aborting replay after " + failures.get() + " failures");
			}
			if (!identities.contains(command.getPrincipal().id())) {
				dispatcher.discard(command);
			} else {
				dispatchWithRetry(command);
			}
			count.incrementAndGet();
		});
		logger.warn(
			"Replayed {} and discarded {} commands out of {} with {} failures in {} s",
			replayed.get(),
			count.get() - replayed.get(),
			repository.size(),
			failures.get(),
			timer.elapsed(TimeUnit.SECONDS)
		);
		if (failures.get() > 0) {
			throw new IllegalStateException("Replay completed with one or more failures");
		}
	}

	private static Set<String> loadIdentities(IndexManager indexManager) {
		var users = new UserRepository(indexManager);
		Set<String> identities = new HashSet<>();
		users.find(new UserQuery(), user -> identities.add(user.getId()));
		return identities;
	}

	private void dispatchWithRetry(Command command) {
		try {
			dispatcher.dispatch(command);
			replayed.incrementAndGet();
		} catch (NonExistentUserException e) {
			logger.warn("Skipping command applying to a non-existent user: {}", command);
		} catch (OpenSearchException e) {
			if (e.status() == 404) {
				logger.warn("Skipping command for deleted bucket: {}", command);
			} else if (e.status() == 409) {
				logger.warn("Skipping duplicate command: {}", command);
			} else {
				logger.error("Couldn't replay command: {}", command, e);
				failures.incrementAndGet();
			}
		} catch (IllegalStateException e) {
			retryCommand(command, e);
		} catch (RuntimeException e) {
			logger.error("Couldn't replay command: {}", command, e);
			failures.incrementAndGet();
		}
	}

	private void retryCommand(Command command, IllegalStateException cause) {
		for (int retry = 1; retry <= 3; retry++) {
			logger.warn("Retrying command (attempt {}): {}", retry, command);
			try {
				Thread.sleep(retry * 1000L);
				dispatcher.dispatch(command);
				replayed.incrementAndGet();
				return;
			} catch (IllegalStateException e) {
				// continue retrying
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		logger.error("Couldn't replay command: {}", command, cause);
		failures.incrementAndGet();
	}
}
