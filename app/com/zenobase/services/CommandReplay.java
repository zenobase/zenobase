package com.zenobase.services;

import javax.inject.Named;

import play.Logger;
import com.google.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;

public class CommandReplay {

	private final String sourceCluster;
	private final NodeFactory nodeFactory;
	private final CommandParserRegistry parsers;
	private final CommandDispatcher dispatcher;

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
		Logger.info("Replaying commands from " + sourceCluster + "...");
		CommandRepository repository = new CommandRepository(indexManager, parsers);
		repository.findAll(new Callback<Command>() {
			@Override
			public void call(Command command) {
				dispatcher.dispatch(command);
			}
		});
	}
}
