package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.EventSearch;
import com.zenobase.testing.NodeAssert;

public class EventRepositoryTest extends ElasticSearchTestSupport {

	private Identity me = new Identity("me");
	private EventRepository repository;

	@Before
	public void setUp() {
		repository = new EventRepository(getManager());
	}

	@Test
	public void testCrudEvent() {

		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket, true);

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
		assertThat(repository.find(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		NodeAssert.assertThat(repository.find(bucket.getId(), new EventSearch())).path(EventSearch.TOTAL.getName()).isEqualTo(1);
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").containsOnly("test", "demo");

		// update event
		event.addValue(Event.TAG, "updated");
		repository.update(bucket.getId(), event);
		repository.refresh(bucket.getId());
		assertThat(repository.find(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").containsOnly("test", "demo", "updated");

		// delete event
		repository.delete(bucket.getId(), event.getId());
		repository.refresh(bucket.getId());
		assertThat(repository.getSize(bucket.getId())).as("bucket size").isZero();
		assertThat(repository.find(bucket.getId(), event.getId())).as("event").isNull();
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").isEmpty();
	}
}
