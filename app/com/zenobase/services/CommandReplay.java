package com.zenobase.services;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import play.Logger;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.StringBloomFilter;

public class CommandReplay {

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

	void replay(final IndexManager indexManager) {
		final CommandRepository repository = new CommandRepository(indexManager, parsers);
		final StringBloomFilter identities = new IdentitiesFilterBuilder(new UserRepository(indexManager)).build();
		Logger.info(String.format("Replaying %d commands from %s...", repository.size(), sourceCluster));
		Stopwatch timer = new Stopwatch().start();
		try {
			repository.find(new Callback<Command>() {
				@Override
				public void call(Command command) {
					if (identities.mightContain(command.getPrincipal().getId())) {
						dispatcher.dispatch(command);
						++replayed;
					} else {
						dispatcher.discard(command);
					}
					++count;
				}
			});
		} catch (RuntimeException e) {
			Logger.error("Couldn't replay a command", e);
			throw e;
		} finally {
			Logger.warn(String.format("Replayed %d and discarded %d commands out of %d in %d s",
				replayed, count - replayed, repository.size(), timer.elapsed(TimeUnit.SECONDS)));
		}
	}
}
