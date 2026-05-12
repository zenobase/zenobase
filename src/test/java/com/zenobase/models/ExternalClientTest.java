package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ExternalClientTest {

	@Test
	public void testCompositeId() {
		assertThat(ExternalClient.id(new Identity("u1"), new Identity("c1"))).isEqualTo("u1|c1");
	}

	@Test
	public void testConstructorFillsFirstSeenAndLastUsed() {
		ExternalClient client = new ExternalClient(new Identity("u1"), new Identity("c1"));
		assertThat(client.getFirstSeen()).isEqualTo(client.getLastUsed());
		assertThat(client.getName()).isNull();
	}

	@Test
	public void testTouchAdvancesLastUsedButNotFirstSeen() throws InterruptedException {
		ExternalClient client = new ExternalClient(new Identity("u1"), new Identity("c1"));
		var originalFirstSeen = client.getFirstSeen();
		var originalLastUsed = client.getLastUsed();
		Thread.sleep(5);
		client.touch();
		assertThat(client.getFirstSeen()).isEqualTo(originalFirstSeen);
		assertThat(client.getLastUsed().isAfter(originalLastUsed)).isTrue();
	}

	@Test
	public void testSetName() {
		ExternalClient client = new ExternalClient(new Identity("u1"), new Identity("c1"));
		client.setName("Claude Desktop");
		assertThat(client.getName()).isEqualTo("Claude Desktop");
	}
}
