package com.zenobase.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Stopwatch;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import play.Logger;
import play.Logger.ALogger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.NonExistentUserException;
import com.zenobase.common.StringFilter;

public class CommandReplay {

	private final ALogger log = Logger.of("replay");
	private static final SearchOrder ORDER = new SearchOrder(Command.TIMESTAMP.getName(), true);

	private final String sourceHost;
	private final int parallelism;
	private final CommandParserRegistry parsers;
	private final CommandDispatcher dispatcher;
	private final AtomicInteger count = new AtomicInteger();
	private final AtomicInteger replayed = new AtomicInteger();
	private final AtomicInteger failures = new AtomicInteger();

	@Inject
	public CommandReplay(@Named("opensearch.replay.host") String sourceHost, @Named("opensearch.replay.parallelism") int parallelism, CommandParserRegistry parsers, CommandDispatcher dispatcher) {
		this.sourceHost = sourceHost;
		this.parallelism = parallelism > 0 ? parallelism : Runtime.getRuntime().availableProcessors();
		this.parsers = parsers;
		this.dispatcher = dispatcher;
	}

	public void replay() {
		if (!sourceHost.isEmpty()) {
			ClientFactory factory = () -> {
				HttpHost httpHost = HttpHost.create(java.net.URI.create(sourceHost));
				return new OpenSearchClient(ApacheHttpClient5TransportBuilder
					.builder(httpHost)
					.setMapper(new JacksonJsonpMapper())
					.build());
			};
			IndexManager indexManager = new IndexManager(factory);
			replay(indexManager);
			indexManager.close();
		}
	}

	void replay(IndexManager indexManager) {
		replay(indexManager, new IdentitiesFilterBuilder(
			new UserRepository(indexManager), new AuthorizationRepository(indexManager)));
	}

	void replay(IndexManager indexManager, IdentitiesFilterBuilder identitiesFilterBuilder) {
		CommandRepository repository = new CommandRepository(indexManager, parsers);
		StringFilter identities = identitiesFilterBuilder.build();
		log.info("Replaying {} commands from {} with {}x...", repository.size(), sourceHost, parallelism);
		Stopwatch timer = Stopwatch.createStarted();
		ExecutorService[] lanes = new ExecutorService[parallelism];
		for (int i = 0; i < parallelism; ++i) {
			lanes[i] = Executors.newSingleThreadExecutor();
		}
		Semaphore semaphore = new Semaphore(parallelism * 100);
		try {
			repository.find(new CommandQuery(), ORDER, command -> {
				if (identities.mightContain(command.getPrincipal().getId())) {
					String principalId = command.getPrincipal().getId();
					int lane = Math.floorMod(principalId.hashCode(), parallelism);
					semaphore.acquireUninterruptibly();
					lanes[lane].submit(() -> {
						try {
							dispatchWithRetry(command);
						} finally {
							semaphore.release();
						}
					});
				} else {
					dispatcher.discard(command);
				}
				count.incrementAndGet();
			});
			for (ExecutorService lane : lanes) {
				lane.shutdown();
			}
			for (ExecutorService lane : lanes) {
				lane.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Replay interrupted", e);
		} finally {
			for (ExecutorService lane : lanes) {
				lane.shutdownNow();
			}
			log.warn("Replayed {} and discarded {} commands out of {} with {} failures in {} s",
				replayed.get(), count.get() - replayed.get(), repository.size(), failures.get(), timer.elapsed(TimeUnit.SECONDS));
		}
		if (failures.get() > 0) {
			throw new IllegalStateException("Replay completed with one or more failures");
		}
	}

	private void dispatchWithRetry(Command command) {
		try {
			dispatcher.dispatch(command);
			replayed.incrementAndGet();
		} catch (NonExistentUserException e) {
			log.warn("Skipping command applying to a non-existent user: " + command);
		} catch (OpenSearchException e) {
			if (e.status() == 404) {
				log.warn("Skipping command for deleted bucket: " + command);
			} else if (e.status() == 409) {
				log.warn("Skipping duplicate command: " + command);
			} else {
				log.error("Couldn't replay command: " + command, e);
				failures.incrementAndGet();
			}
		} catch (IllegalStateException e) {
			retryCommand(command, e);
		} catch (RuntimeException e) {
			log.error("Couldn't replay command: " + command, e);
			failures.incrementAndGet();
		}
	}

	private void retryCommand(Command command, IllegalStateException cause) {
		for (int retry = 1; retry <= 3; retry++) {
			log.warn("Retrying command (attempt {}): {}", retry, command);
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
		log.error("Couldn't replay command: " + command, cause);
		failures.incrementAndGet();
	}
}
