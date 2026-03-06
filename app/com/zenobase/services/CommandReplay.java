package com.zenobase.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Stopwatch;
import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.NonExistentUserException;
import com.zenobase.common.StringFilter;

public class CommandReplay {

	private static final SearchOrder ORDER = new SearchOrder(Command.TIMESTAMP.getName(), true);

	private final String sourceCluster;
	private final int parallelism;
	private final NodeFactory nodeFactory;
	private final CommandParserRegistry parsers;
	private final CommandDispatcher dispatcher;

	@Inject
	public CommandReplay(@Named("es.replay.cluster") String sourceCluster, @Named("es.replay.parallelism") int parallelism, NodeFactory nodeFactory, CommandParserRegistry parsers, CommandDispatcher dispatcher) {
		this.sourceCluster = sourceCluster;
		this.parallelism = parallelism;
		this.nodeFactory = nodeFactory;
		this.parsers = parsers;
		this.dispatcher = dispatcher;
	}

	public void replay() {
		if (!sourceCluster.isEmpty()) {
			IndexManager indexManager = new IndexManager(nodeFactory, sourceCluster);
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
		Logger.info("Replaying {} commands from {} with {}x...", repository.size(), sourceCluster, parallelism);
		Stopwatch timer = Stopwatch.createStarted();
		AtomicInteger count = new AtomicInteger();
		AtomicInteger replayed = new AtomicInteger();
		AtomicInteger failures = new AtomicInteger();
		ExecutorService[] lanes = new ExecutorService[parallelism];
		for (int i = 0; i < parallelism; ++i) {
			lanes[i] = Executors.newSingleThreadExecutor();
		}
		try {
			repository.find(new CommandQuery(), ORDER, command -> {
				if (identities.mightContain(command.getPrincipal().getId())) {
					String principalId = command.getPrincipal().getId();
					int lane = Math.floorMod(principalId.hashCode(), parallelism);
					lanes[lane].submit(() -> {
						try {
							dispatcher.dispatch(command);
							replayed.incrementAndGet();
						} catch (NonExistentUserException e) {
							Logger.warn("Skipping command applying to a non-existent user: " + command);
						} catch (DocumentAlreadyExistsException e) {
							Logger.warn("Skipping duplicate command: " + command);
						} catch (RuntimeException e) {
							Logger.error("Couldn't replay command: " + command, e);
							failures.incrementAndGet();
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
			Logger.warn("Replayed {} and discarded {} commands out of {} with {} failures in {} s",
				replayed.get(), count.get() - replayed.get(), repository.size(), failures.get(), timer.elapsed(TimeUnit.SECONDS));
		}
		if (failures.get() > 0) {
			throw new IllegalStateException("Replay completed with one or more failures");
		}
	}
}
