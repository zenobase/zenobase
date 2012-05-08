package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Callback;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;
import com.zenobase.testing.NodeAssert;

public class BucketRepositoryTest extends ElasticSearchTestSupport {

	private Identity me = new Identity("me");
	private Identity you = new Identity("you");
	private BucketRepository repository;

	@Before
	public void setUp() {
		repository = new BucketRepository(getManager());
	}

	@Test
	public void testCrudBucket() {

		// create bucket
		Bucket bucket = newBucket("Test Bucket", me);
		bucket.setWidgets(ImmutableList.of(newWidget()));

		// store and retrieve bucket
		repository.store(bucket, true);
		assertThat(repository.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// update bucket
		bucket.setDescription("just a test");
		repository.update(bucket);
		assertThat(repository.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// delete and recreate bucket
		assertThat(repository.deleteBucket(bucket.getId())).isTrue();
		assertThat(repository.findBucket(bucket.getId())).as("bucket").isNull();
		assertThat(repository.deleteBucket(bucket.getId())).isFalse();
		repository.store(bucket, false);
		assertThat(repository.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());
	}

	private static ObjectNode newWidget() {
		ObjectNode widget = Nodes.newObject();
		widget.put("option", true);
		return widget;
	}

	@Test
	public void testFindBuckets() {

		List<Bucket> buckets = newBucketList(20);
		for (Bucket bucket : buckets) {
			repository.store(bucket, true);
		}

		assertThat(repository.findBuckets(0, 10)).hasSize(buckets.size()).isEqualTo(buckets.subList(0, 10));
		assertThat(repository.findBuckets(me, 0, 10)).hasSize(buckets.size() / 2);
		assertThat(repository.findBuckets(you, 0, 10)).hasSize(buckets.size() / 2);
	}

	@Test
	public void testScrollBuckets() {

		List<Bucket> buckets = newBucketList(15); // large enough to require scrolling
		for (Bucket bucket : buckets) {
			repository.store(bucket, true);
		}

		Callback<Bucket> callback = mock(Callback.class);
		repository.findBuckets(callback);
		verify(callback, times(buckets.size())).call(any(Bucket.class));
	}

	@Test
	public void testCrudEvent() {

		Bucket bucket = newBucket("Test", me);
		repository.store(bucket, true);

		// create event
		Event event = new Event();
		event.setValue(Event.AUTHOR, me);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.addValue(Event.TAG, "test");
		event.addValue(Event.TAG, "demo");

		// store and retrieve event
		assertThat(repository.getSize(bucket.getId())).as("bucket size").isZero();
		repository.add(bucket.getId(), event);
		repository.refresh(bucket.getId());
		assertThat(repository.getSize(bucket.getId())).as("bucket size").isEqualTo(1L);
		assertThat(repository.findEvent(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		NodeAssert.assertThat(repository.findEvents(bucket.getId(), new EventSearch())).path(EventSearch.TOTAL.getName()).isEqualTo(1);

		// delete event
		repository.delete(bucket.getId(), event.getId());
		repository.refresh(bucket.getId());
		assertThat(repository.getSize(bucket.getId())).as("bucket size").isZero();
		assertThat(repository.findEvent(bucket.getId(), event.getId())).as("event").isNull();
	}

	private static Bucket newBucket(String label, Identity owner) {
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.addPermission(owner, Permission.ALL);
		return bucket;
	}

	private List<Bucket> newBucketList(int size) {
		Preconditions.checkArgument(size < 100);
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		for (int i = 0; i < size; ++i) {
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // to allow buckets to be sorted
			buckets.add(newBucket(String.format("bucket%03d", i + 1), i % 2 == 0 ? me : you));
		}
		return buckets.build().reverse();
	}
}
