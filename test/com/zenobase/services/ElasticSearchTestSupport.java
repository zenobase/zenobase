package com.zenobase.services;

import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.zenobase.common.Callback;

public abstract class ElasticSearchTestSupport {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private String clusterName = "test";
	private NodeFactory nodeFactory;
	private IndexManager manager;

	@Before
	public void createManager() {
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
	public void closeManager() {
		manager.close();
	}

	protected static <T> void verifyInteractions(Callback<T> callback, Iterable<T> expected) {
		for (T t : expected) {
			verify(callback).call(t);
		}
		verifyNoMoreInteractions(callback);
	}
}
