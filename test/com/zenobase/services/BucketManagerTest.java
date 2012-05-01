package com.zenobase.services;

import static com.zenobase.test.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

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

public class BucketManagerTest extends ElasticSearchTestSupport {

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

		IndexManager indexManager = mock(IndexManager.class);
		Index bucketIndex = new Index(BucketManager.INDEX_NAME, getClient());
		Index eventIndex = new Index(bucket.getId(), getClient());
		when(indexManager.getIndex(BucketManager.INDEX_NAME)).thenReturn(bucketIndex);
		when(indexManager.getIndex(bucket.getId())).thenReturn(eventIndex);
		BucketManager manager = new BucketManager(indexManager);

		// store and retrieve bucket
		manager.store(bucket, true);
		assertThat(manager.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// update bucket
		String description = "just a test";
		bucket.setDescription(description);
		manager.update(bucket);
		assertThat(manager.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// delete and recreate bucket
		manager.deleteBucket(bucket.getId());
		assertThat(manager.findBucket(bucket.getId())).as("bucket").isNull();
		manager.store(bucket, false);
		assertThat(manager.findBucket(bucket.getId()).toJson()).isEqualTo(bucket.toJson());

		// create event
		Event event = new Event();
		event.setValue(Event.AUTHOR, principal);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.addValue(Event.TAG, "test");
		event.addValue(Event.TAG, "demo");

		// store and retrieve event
		assertThat(manager.getSize(bucket.getId())).as("bucket size").isZero();
		manager.add(bucket.getId(), event);
		eventIndex.refresh();
		assertThat(manager.getSize(bucket.getId())).as("bucket size").isEqualTo(1L);
		assertThat(manager.findEvent(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());

		// delete event
		manager.delete(bucket.getId(), event.getId());
		eventIndex.refresh();
		assertThat(manager.getSize(bucket.getId())).as("bucket size").isZero();
		assertThat(manager.findEvent(bucket.getId(), event.getId())).as("event").isNull();
	}
}
