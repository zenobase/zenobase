package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class BucketManagerTest extends ElasticSearchTestSupport {

	@Test
	public void test() {

		// create bucket
		String bucketId = Generator.id();
		String label = "Test Bucket";
		Identity principal = new Identity("me");
		ObjectNode widget = Nodes.newObject();
		ImmutableList<ObjectNode> widgets = ImmutableList.of(widget);
		widget.put("option", true);
		Bucket bucket = new Bucket(bucketId);
		bucket.setLabel(label);
		bucket.addPermission(principal, Permission.ALL);
		bucket.setWidgets(widgets);

		IndexManager indexManager = mock(IndexManager.class);
		Index bucketIndex = new Index(BucketManager.INDEX_NAME, getClient());
		Index eventIndex = new Index(bucketId, getClient());
		when(indexManager.getIndex(BucketManager.INDEX_NAME)).thenReturn(bucketIndex);
		when(indexManager.getIndex(bucketId)).thenReturn(eventIndex);
		BucketManager manager = new BucketManager(indexManager);

		// store and retrieve bucket
		manager.store(bucket, true);
		assertThat((Object) manager.findBucket(bucketId).toJson()).isEqualTo(bucket.toJson());

		// update bucket
		String description = "just a test";
		bucket.setDescription(description);
		manager.update(bucket);
		assertThat((Object) manager.findBucket(bucketId).toJson()).isEqualTo(bucket.toJson());

		// delete and recreate bucket
		manager.deleteBucket(bucketId);
		assertThat(manager.findBucket(bucketId)).as("bucket").isNull();
		manager.store(bucket, false);
		assertThat((Object) manager.findBucket(bucketId).toJson()).isEqualTo(bucket.toJson());

		// create event
		String eventId = Generator.id();
		Event event = new Event(eventId);
		event.setValue(Event.AUTHOR, principal);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.addValue(Event.TAG, "test");
		event.addValue(Event.TAG, "demo");

		// store and retrieve event
		assertThat(manager.getSize(bucketId)).as("bucket size").isZero();
		manager.add(bucketId, event);
		eventIndex.refresh();
		assertThat(manager.getSize(bucketId)).as("bucket size").isEqualTo(1L);
		assertThat((Object) manager.findEvent(bucketId, eventId).toJson()).isEqualTo(event.toJson());

		// delete event
		manager.delete(bucketId, eventId);
		eventIndex.refresh();
		assertThat(manager.getSize(bucketId)).as("bucket size").isZero();
		assertThat(manager.findEvent(bucketId, eventId)).as("event").isNull();
	}
}
