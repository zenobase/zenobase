package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ExternalClientTest {

	@Test
	public void testCompositeId() {
		assertThat(ExternalClient.id(new Identity("u1"), new Identity("c1"))).isEqualTo("u1|c1");
	}

	@Test
	public void testConstructorFillsRequiredFields() {
		DateTime firstSeen = new DateTime(2026, 5, 1, 12, 0, DateTimeZone.UTC);
		ExternalClient client = new ExternalClient(new Identity("u1"), new Identity("c1"), "Claude Desktop", firstSeen);
		assertThat(client.getId()).isEqualTo("u1|c1");
		assertThat(client.getUser().id()).isEqualTo("u1");
		assertThat(client.getClient().id()).isEqualTo("c1");
		assertThat(client.getName()).isEqualTo("Claude Desktop");
		assertThat(client.getFirstSeen()).isEqualTo(firstSeen);
		assertThat(client.getReadableBuckets()).isEmpty();
	}

	@Test
	public void testReadableBucketsRoundTrip() {
		ExternalClient client = new ExternalClient(
			new Identity("u1"),
			new Identity("c1"),
			null,
			new DateTime(2026, 5, 1, 12, 0, DateTimeZone.UTC)
		);
		client.setReadableBuckets(java.util.List.of("b1", "b2", "b3"));
		assertThat(client.getReadableBuckets()).containsExactly("b1", "b2", "b3");

		ExternalClient roundTripped = new ExternalClient(client.toJson().deepCopy());
		assertThat(roundTripped.getReadableBuckets()).containsExactly("b1", "b2", "b3");
	}

	@Test
	public void testCanRead() {
		ExternalClient client = new ExternalClient(
			new Identity("u1"),
			new Identity("c1"),
			null,
			new DateTime(2026, 5, 1, 12, 0, DateTimeZone.UTC)
		);
		client.setReadableBuckets(java.util.List.of("b1", "b2"));
		assertThat(client.canRead("b1")).isTrue();
		assertThat(client.canRead("b2")).isTrue();
		assertThat(client.canRead("b3")).isFalse();
	}

	@Test
	public void testNameDefaultsToNull() {
		ExternalClient client = new ExternalClient(
			new Identity("u1"),
			new Identity("c1"),
			null,
			new DateTime(2026, 5, 1, 12, 0, DateTimeZone.UTC)
		);
		assertThat(client.getName()).isNull();
		client.setName("Claude Desktop");
		assertThat(client.getName()).isEqualTo("Claude Desktop");
	}
}
