package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ExternalBucketGrantTest {

	@Test
	public void testCompositeId() {
		Identity user = new Identity("u1");
		Identity client = new Identity("c1");
		assertThat(ExternalBucketGrant.id(user, client, "b1")).isEqualTo("u1|c1|b1");
	}

	@Test
	public void testConstructorFillsAllRequiredFields() {
		Identity user = new Identity("u1");
		Identity client = new Identity("c1");
		DateTime created = new DateTime(2026, 4, 1, 12, 0, DateTimeZone.UTC);
		ExternalBucketGrant grant = new ExternalBucketGrant(
			user,
			client,
			"b1",
			ExternalBucketGrant.RIGHT_READ,
			created
		);

		assertThat(grant.getId()).isEqualTo("u1|c1|b1");
		assertThat(grant.getUser()).isEqualTo(user);
		assertThat(grant.getClient()).isEqualTo(client);
		assertThat(grant.getBucketId()).isEqualTo("b1");
		assertThat(grant.getRights()).isEqualTo("read");
		assertThat(grant.getCreated()).isEqualTo(created);
	}

	@Test
	public void testRoundTripThroughJson() {
		Identity user = new Identity("u1");
		Identity client = new Identity("c1");
		ExternalBucketGrant grant = new ExternalBucketGrant(user, client, "b1", "read");

		ExternalBucketGrant clone = new ExternalBucketGrant(grant.toJson().deepCopy());

		assertThat(clone.getId()).isEqualTo(grant.getId());
		assertThat(clone.getUser()).isEqualTo(user);
		assertThat(clone.getClient()).isEqualTo(client);
		assertThat(clone.getBucketId()).isEqualTo("b1");
		assertThat(clone.getRights()).isEqualTo("read");
	}
}
