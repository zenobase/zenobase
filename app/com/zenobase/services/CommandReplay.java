package com.zenobase.services;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Stopwatch;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.StringBloomFilter;

public class CommandReplay {

	private static final SearchOrder ORDER = new SearchOrder(Command.TIMESTAMP.getName(), true);

	private final String sourceCluster;
	private final NodeFactory nodeFactory;
	private final CommandParserRegistry parsers;
	private final CommandDispatcher dispatcher;
	private int count, replayed;

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
				} catch (RuntimeException e) {
					Logger.error("Couldn't replay command: " + command, e);
					throw e;
				}
			});
		} finally {
			Logger.warn("Replayed {} and discarded {} commands out of {} in {} s",
				replayed, count - replayed, repository.size(), timer.elapsed(TimeUnit.SECONDS));
		}
	}
}
