package com.zenobase.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class ElasticSearchTestSupport {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private String clusterName = "test";
	private NodeFactory nodeFactory;
	private IndexManager manager;

	@Before
	public void init() {
		nodeFactory = new TestNodeFactory(folder.getRoot());
		manager = new IndexManager(nodeFactory, clusterName);
	}

	protected NodeFactory getNodeFactory() {
		return nodeFactory;
	}

	protected IndexManager getManager() {
		return manager;
	}

	protected String getClusterName() {
		return clusterName;
	}

	@After
	public void close() {
		manager.close();
	}
}
