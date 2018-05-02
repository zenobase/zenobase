package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.testing.NodeAssert;

public class VirtualBucketTest extends ElasticSearchTestSupport {

	private Identity me = new Identity("me");
	private BucketRepository buckets;
	private EventRepository events;

	@Before
	public void setUp() {
		buckets = new BucketRepository(getManager());
		events = new EventRepository(getManager());
	}

	@Test
	public void test() {

		Bucket b1 = newBucket("First Bucket", me);
		buckets.store(b1, DateTime.now(), true);
		assertThat(buckets.isAliased(b1.getId())).isFalse();

		Bucket b2 = newBucket("Second Bucket", me);
		buckets.store(b2, DateTime.now(), true);
		assertThat(buckets.isAliased(b2.getId())).isFalse();

		Bucket b3 = newBucket("My Data", me);
		b3.addAlias(new Alias(b1.getId()));
		b3.addAlias(new Alias(b2.getId(), "tag:bar"));
		buckets.store(b3, DateTime.now(), true);
		assertThat(buckets.isAliased(b1.getId())).isTrue();
		assertThat(buckets.isAliased(b2.getId())).isTrue();
		assertThat(buckets.isAliased(b3.getId())).isFalse();
		NodeAssert.assertThat(buckets.find(b3.getId()).toJson()).isEqualTo(b3.toJson());

		events.add(b1.getId(), newEvent("foo"), DateTime.now());
		events.add(b2.getId(), newEvent("bar"), DateTime.now());
		events.add(b2.getId(), newEvent("baz"), DateTime.now());
		events.refresh(b1.getId());
		events.refresh(b2.getId());

		assertThat(events.size(b1.getId())).isEqualTo(1);
		assertThat(events.size(b2.getId())).isEqualTo(2);
		assertThat(events.size(b3.getId())).isEqualTo(2);

		assertThat(buckets.delete(b3.getId())).isTrue();
		assertThat(buckets.isAliased(b1.getId())).isFalse();
		assertThat(buckets.isAliased(b2.getId())).isFalse();
	}

	private static Bucket newBucket(String label, Identity owner) {
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.addRole(owner, Role.OWNER);
		return bucket;
	}

	private static Event newEvent(String tag) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		return event;
	}
}
