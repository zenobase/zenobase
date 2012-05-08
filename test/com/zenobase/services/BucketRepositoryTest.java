package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;
import com.zenobase.testing.NodeAssert;

public class BucketRepositoryTest extends ElasticSearchTestSupport {

	@Test
	public void testCrudBucket() {

		// create bucket
		String label = "Test Bucket";
		Identity principal = new Identity("me");
		ObjectNode widget = Nodes.newObject();
		ImmutableList<ObjectNode> widgets = ImmutableList.of(widget);
		widget.put("option", true);
		Bucket bucket = newBucket(label, principal);
		bucket.setWidgets(widgets);

		BucketRepository repository = new BucketRepository(getManager());

		// store and retrieve bucket
		repository.store(bucket, true);
		assertThat(repository.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// update bucket
		String description = "just a test";
		bucket.setDescription(description);
		repository.update(bucket);
		assertThat(repository.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// delete and recreate bucket
		repository.deleteBucket(bucket.getId());
		assertThat(repository.findBucket(bucket.getId())).as("bucket").isNull();
		repository.store(bucket, false);
		assertThat(repository.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());
	}

	@Test
	public void testFindBuckets() {
		Identity me = new Identity("me");
		Identity you = new Identity("you");
		List<Bucket> buckets = ImmutableList.of(newBucket("My Bucket", me), newBucket("Also My Bucket", me), newBucket("Your Bucket", you));
		BucketRepository repository = new BucketRepository(getManager());
		for (Bucket bucket : buckets) {
			repository.store(bucket, true);
		}
		assertThat(repository.findBuckets(0, 10)).hasSize(3).isEqualTo(buckets);
		assertThat(repository.findBuckets(me, 0, 10)).hasSize(2).isEqualTo(buckets.subList(0, 2));
		assertThat(repository.findBuckets(you, 0, 10)).hasSize(1).isEqualTo(buckets.subList(2, 3));
	}

	@Test
	public void testCrudEvent() {

		Identity principal = new Identity("me");
		BucketRepository repository = new BucketRepository(getManager());
		Bucket bucket = newBucket("Test", principal);
		repository.store(bucket, true);

		// create event
		Event event = new Event();
		event.setValue(Event.AUTHOR, principal);
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
}
