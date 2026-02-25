package com.zenobase.services;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Stopwatch;
import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.NonExistentUserException;
import com.zenobase.common.StringBloomFilter;

public class CommandReplay {

	private static final SearchOrder ORDER = new SearchOrder(Command.TIMESTAMP.getName(), true);

	private final String sourceCluster;
	private final NodeFactory nodeFactory;
	private final CommandParserRegistry parsers;
	private final CommandDispatcher dispatcher;
	private int count, replayed, failures;

	@Inject
	public CommandReplay(@Named("es.replay") String sourceCluster, NodeFactory nodeFactory, CommandParserRegistry parsers, CommandDispatcher dispatcher) {
		this.sourceCluster = sourceCluster;
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
		CommandRepository repository = new CommandRepository(indexManager, parsers);
		StringBloomFilter identities = new IdentitiesFilterBuilder(new UserRepository(indexManager), new AuthorizationRepository(indexManager)).build();
		Logger.info("Replaying {} commands from {}...", repository.size(), sourceCluster);
		Stopwatch timer = Stopwatch.createStarted();
		try {
			repository.find(new CommandQuery(), ORDER, command -> {
				try {
					if (identities.mightContain(command.getPrincipal().getId())) {
						dispatcher.dispatch(command);
						++replayed;
					} else {
						dispatcher.discard(command);
					}
					++count;
				} catch (NonExistentUserException e) {
					Logger.warn("Skipping command applying to a non-existent user: " + command);
				} catch (DocumentAlreadyExistsException e) {
					Logger.warn("Skipping duplicate command: " + command);
				} catch (RuntimeException e) {
					Logger.error("Couldn't replay command: " + command, e);
					++failures;
				}
			});
		} finally {
			Logger.warn("Replayed {} and discarded {} commands out of {} with {} failures in {} s",
				replayed, count - replayed, repository.size(), failures, timer.elapsed(TimeUnit.SECONDS));
		}
		if (failures > 0) {
			throw new IllegalStateException("Replay completed with one or more failures");
		}
	}
}
