package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Lists;

import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.testing.NodeAssert;

public class EventRepositoryTest extends ElasticSearchTestSupport {

	private Identity me = new Identity("me");
	private EventRepository repository;

	@Before
	public void setUp() {
		repository = new EventRepository(getManager());
	}

	@Test
	public void testCRUD() {

		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket, DateTime.now(), true);

		// create event
		Event event = new Event();
		event.setValue(Event.AUTHOR, me);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.addValue(Event.TAG, "test");
		event.addValue(Event.TAG, "demo");

		// store and retrieve event
		assertThat(repository.size()).as("repository size").isZero();
		assertThat(repository.size(me)).as("event count for user").isZero();
		assertThat(repository.size(bucket.getId())).as("bucket size").isZero();
		repository.add(bucket.getId(), event, DateTime.now());
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isEqualTo(1L);
		assertThat(repository.size(me)).as("event count for user").isEqualTo(1L);
		assertThat(repository.size(bucket.getId())).as("bucket size").isEqualTo(1L);
		assertThat(repository.find(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		NodeAssert.assertThat(repository.find(bucket.getId(), new EventSearchBuilder().buildSearch())).path(Search.TOTAL.getName()).isEqualTo(1);
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").containsOnly("test", "demo");

		// update event
		event.addValue(Event.TAG, "updated");
		repository.update(bucket.getId(), event, DateTime.now());
		repository.refresh(bucket.getId());
		assertThat(repository.find(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").containsOnly("test", "demo", "updated");

		// delete event
		repository.delete(bucket.getId(), event.getId());
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isZero();
		assertThat(repository.size(me)).as("event count for user").isZero();
		assertThat(repository.size(bucket.getId())).as("bucket size").isZero();
		assertThat(repository.find(bucket.getId(), event.getId())).as("event").isNull();
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").isEmpty();
	}


	@Test
	public void testBulk() {

		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket, DateTime.now(), true);

		// create events
		Event e1 = new Event();
		e1.setValue(Event.AUTHOR, me);
		e1.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		e1.addValue(Event.TAG, "foo");
		Event e2 = new Event();
		e2.setValue(Event.AUTHOR, me);
		e2.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		e2.addValue(Event.TAG, "bar");

		// add events
		repository.add(bucket.getId(), Lists.newArrayList(e1, e2), DateTime.now());
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isEqualTo(2L);

		// remove events
		repository.delete(bucket.getId(), Lists.newArrayList(e1.getId(), e2.getId()));
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isZero();
	}

	@Test
	public void testTimestamp() {

		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket, DateTime.now(), true);
		Event event = new Event();
		event.setValue(Event.AUTHOR, me);
		event.addValue(Event.TAG, "test");
		event.toJson().put("timestamp", "2012-10-24T13:10:00.000-00:00");
		repository.add(bucket.getId(), event, DateTime.now());
	}
}
