package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

import com.zenobase.common.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class BucketManagerTest extends ElasticSearchTestSupport {

	@Test
	public void test() {

		String bucketId = "bucket-1";
		String label = "Test Bucket";
		Identity principal = new Identity("me");
		ObjectNode widget = Nodes.newObject();
		ImmutableList<ObjectNode> widgets = ImmutableList.of(widget);
		widget.put("option", true);
		Bucket bucket = new Bucket(bucketId);
		bucket.setLabel(label);
		bucket.addPermission(principal, Permission.ALL);
		bucket.setWidgets(widgets);
		IndexManager indexes = mock(IndexManager.class);
		Index bucketIndex = new Index(BucketManager.INDEX_NAME, getClient());
		Index eventIndex = new Index(bucketId, getClient());
		when(indexes.getIndex(BucketManager.INDEX_NAME)).thenReturn(bucketIndex);
		when(indexes.getIndex(bucketId)).thenReturn(eventIndex);
		BucketManager manager = new BucketManager(indexes);

		// store and retrieve bucket
		manager.store(bucket, true);
		Bucket found = manager.findBucket(bucketId);
		assertThat((Object) found.toJson()).isEqualTo(bucket.toJson());

		// update bucket desc
		String description = "just a test";
		bucket.setDescription(description);
		manager.update(bucket);
		found = manager.findBucket(bucketId);
		assertThat((Object) found.toJson()).isEqualTo(bucket.toJson());

		// delete and recreate bucket
		manager.deleteBucket(bucketId);
		assertThat(manager.findBucket(bucketId)).as("bucket").isNull();
		manager.store(bucket, false);
		found = manager.findBucket(bucketId);
		assertThat((Object) found.toJson()).isEqualTo(bucket.toJson());

		// add event
		assertThat(manager.getSize(bucketId)).as("bucket size").isZero();
		String eventId = "event-1";
		Event e1 = new Event(eventId);
		e1.setValue(Event.AUTHOR, principal);
		e1.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		manager.add(bucketId, e1);
		eventIndex.refresh();
		assertThat(manager.getSize(bucketId)).as("bucket size").isEqualTo(1L);
		Event foundEvent = manager.findEvent(bucketId, eventId);
		assertThat((Object) foundEvent.toJson()).isEqualTo(e1.toJson());

		// delete event
		manager.delete(bucketId, eventId);
		eventIndex.refresh();
		assertThat(manager.getSize(bucketId)).as("bucket size").isZero();
	}
}
