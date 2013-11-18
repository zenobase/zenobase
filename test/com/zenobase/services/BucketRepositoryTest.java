package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Callback;
import com.zenobase.json.Nodes;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class BucketRepositoryTest extends ElasticSearchTestSupport {

	private static final Identity ME = new Identity("me");
	private static final Identity YOU = new Identity("you");

	private BucketRepository repository;

	@Before
	public void setUp() {
		repository = new BucketRepository(getManager());
	}

	@Test
	public void test() {

		// create bucket
		Bucket bucket = newBucket("Test Bucket", ME);
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
	public void testFindWithPaging() {
		List<Bucket> expected = Lists.reverse(insert(11));
		assertThat(repository.find(0, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(20, 10)).hasTotal(expected.size()).isEmpty();
	}

	@Test
	public void testFindWithPagingInReverse() {
		List<Bucket> expected = insert(11);
		SearchOrder order = BucketQuery.DEFAULT_ORDER.reverse();
		assertThat(repository.find(new BucketQuery(), order, 0, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(new BucketQuery(), order, 10, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(new BucketQuery(), order, 20, 10)).hasTotal(expected.size()).isEmpty();
	}

	@Test
	public void testFindWithCallback() {
		List<Bucket> expected = insert(11);
		Callback<Bucket> callback = mock(Callback.class);
		repository.find(new BucketQuery(), callback);
		verifyInteractions(callback, expected);
	}

	@Test
	public void testFindPrincipalEqualTo() {
		Bucket b1 = insert("foo", ME);
		Bucket b2 = insert("bar", ME);
		insert("foo", YOU);
		Callback<Bucket> callback = mock(Callback.class);
		repository.find(new BucketQuery().principalEqualTo(ME), callback);
		verifyInteractions(callback, ImmutableList.of(b1, b2));
	}

	@Test
	public void testFindIsAlias() {

		Bucket b1 = insert("foo", ME);
		Bucket b2 = insert("bar", ME, b1.getId());
		assertThat(repository.isAliased(b1.getId())).isTrue();
		assertThat(repository.isAliased(b2.getId())).isFalse();

		Callback<Bucket> c1 = mock(Callback.class);
		repository.find(new BucketQuery().isAlias(true), c1);
		verifyInteractions(c1, ImmutableList.of(b2));

		Callback<Bucket> c2 = mock(Callback.class);
		repository.find(new BucketQuery().isAlias(false), c2);
		verifyInteractions(c2, ImmutableList.of(b1));
	}

	private List<Bucket> insert(int size) {
		List<Bucket> buckets = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // to allow buckets to be sorted
			Bucket bucket = newBucket(String.format("bucket%03d", i + 1), ME);
			buckets.add(bucket);
			repository.store(bucket, DateTime.now(), true);
		}
		return buckets;
	}

	private Bucket insert(String label, Identity owner) {
		return insert(label, owner, null);
	}

	private Bucket insert(String label, Identity owner, String aliasId) {
		Bucket bucket = newBucket(label, owner, aliasId);
		repository.store(bucket, DateTime.now(), true);
		return bucket;
	}

	private static Bucket newBucket(String label, Identity owner) {
		return newBucket(label, owner, null);
	}

	private static Bucket newBucket(String label, Identity owner, String aliasId) {
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.addRole(owner, Role.OWNER);
		if (aliasId != null) {
			bucket.addAlias(new Alias(aliasId));
		}
		return bucket;
	}
}
