package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Callback;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;

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
		repository.store(bucket, DateTime.now(), true);
		assertThat(repository.find(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// update bucket
		bucket.setDescription("just a test");
		repository.update(bucket, DateTime.now());
		assertThat(repository.find(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// delete and recreate bucket
		assertThat(repository.delete(bucket.getId())).isTrue();
		assertThat(repository.find(bucket.getId())).as("bucket").isNull();
		assertThat(repository.delete(bucket.getId())).isFalse();
		repository.store(bucket, DateTime.now(), false);
		assertThat(repository.find(bucket.getId()).toJson()).isEqualTo(bucket.toJson());
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
			repository.store(bucket, DateTime.now(), true);
		}

		assertThat(repository.find(0, 10)).hasTotal(buckets.size()).isEqualTo(buckets.subList(0, 10));
		assertThat(repository.find(me, 0, 10)).hasTotal(buckets.size() / 2);
		assertThat(repository.find(you, 0, 10)).hasTotal(buckets.size() / 2);
	}

	@Test
	public void testScrollBuckets() {

		List<Bucket> buckets = newBucketList(15); // large enough to require scrolling
		for (Bucket bucket : buckets) {
			repository.store(bucket, DateTime.now(), true);
		}

		Callback<Bucket> callback = mock(Callback.class);
		repository.findAll(callback);
		verify(callback, times(buckets.size())).call(any(Bucket.class));
	}

	private static Bucket newBucket(String label, Identity owner) {
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.addRole(owner, Role.OWNER);
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
