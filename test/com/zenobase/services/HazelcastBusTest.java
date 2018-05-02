package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HazelcastBusTest {

	private Bus hazelcast;

	@Before
	public void setUp() {
		hazelcast = new HazelcastBus();
	}

	@Test
	public void testToggleReadOnly() {
		assertThat(hazelcast.isReadOnly()).isFalse();
		hazelcast.setReadOnly(true);
		assertThat(hazelcast.isReadOnly()).isTrue();
		hazelcast.setReadOnly(false);
		assertThat(hazelcast.isReadOnly()).isFalse();
	}

	@Test
	public void testMaster() {
		assertThat(hazelcast.isMaster()).isTrue();
	}

	@After
	public void tearDown() {
		hazelcast.close();
	}
}
