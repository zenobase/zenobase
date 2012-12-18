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
		CommandRepository repository = new CommandRepository(indexManager, parsers);
		Logger.info("Replaying " + repository.size() + " commands from " + sourceCluster + "...");
		final StringBloomFilter identities = new IdentitiesFilterBuilder(new UserRepository(indexManager)).build();
		Stopwatch timer = new Stopwatch().start();
		try {
			repository.findAll(new Callback<Command>() {
				@Override
				public void call(Command command) {
					if (identities.mightContain(command.getPrincipal().getId())) {
						dispatcher.dispatch(command);
						++replayed;
					}
					++count;
				}
			});
		} finally {
			timer.stop();
			Logger.warn("Replayed " + replayed + "/" + count + " in " + timer.elapsedTime(TimeUnit.SECONDS) + " s");
		}
	}
}
