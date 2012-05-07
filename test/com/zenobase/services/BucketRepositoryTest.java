package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

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

public class BucketRepositoryTest extends ElasticSearchTestSupport {

	@Test
	public void test() {

		// create bucket
		String label = "Test Bucket";
		Identity principal = new Identity("me");
		ObjectNode widget = Nodes.newObject();
		ImmutableList<ObjectNode> widgets = ImmutableList.of(widget);
		widget.put("option", true);
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.addPermission(principal, Permission.ALL);
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

		// delete event
		repository.delete(bucket.getId(), event.getId());
		repository.refresh(bucket.getId());
		assertThat(repository.getSize(bucket.getId())).as("bucket size").isZero();
		assertThat(repository.findEvent(bucket.getId(), event.getId())).as("event").isNull();
	}
}
