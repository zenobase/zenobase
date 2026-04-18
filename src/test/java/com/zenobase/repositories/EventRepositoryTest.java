package com.zenobase.repositories;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.testing.NodeAssert;

public class EventRepositoryTest extends OpenSearchTestSupport {

	private Identity me = new Identity("me");
	private EventRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new EventRepository(getManager());
	}

	@Test
	public void testCRUD() {
		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket);

		// create event
		Event event = new Event();
		event.setValue(Event.AUTHOR, me);
		event.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		event.addValue(Event.TAG, "test");
		event.addValue(Event.TAG, "demo");

		// store and retrieve event
		assertThat(repository.size()).as("repository size").isZero();
		assertThat(repository.size(me)).as("event count for user").isZero();
		assertThat(repository.size(bucket.getId())).as("bucket size").isZero();
		repository.add(bucket.getId(), event);
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isEqualTo(1L);
		assertThat(repository.size(me)).as("event count for user").isEqualTo(1L);
		assertThat(repository.size(bucket.getId())).as("bucket size").isEqualTo(1L);
		assertThat(repository.find(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		NodeAssert.assertThat(repository.find(bucket.getId(), new EventSearchBuilder().buildSearch()))
			.path(Search.TOTAL.getName())
			.isEqualTo(1);
		List<Event> all = new ArrayList<>();
		repository.findAll(bucket.getId(), all::add);
		assertThat(all).as("event count in bucket").hasSize(1);
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName())).as("tags").containsOnly("test", "demo");

		// update event
		Event before = event.copy();
		event.addValue(Event.TAG, "updated");
		repository.update(bucket.getId(), before, event);
		repository.refresh(bucket.getId());
		assertThat(repository.find(bucket.getId(), event.getId()).toJson()).isEqualTo(event.toJson());
		assertThat(repository.terms(bucket.getId(), Event.TAG.getName()))
			.as("tags")
			.containsOnly("test", "demo", "updated");

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
		new BucketRepository(getManager()).store(bucket);

		// create events
		Event e1 = new Event();
		e1.setValue(Event.AUTHOR, me);
		e1.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		e1.addValue(Event.TAG, "foo");
		Event e2 = new Event();
		e2.setValue(Event.AUTHOR, me);
		e2.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		e2.addValue(Event.TAG, "bar");

		// add events
		repository.add(bucket.getId(), Lists.newArrayList(e1, e2));
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isEqualTo(2L);

		// remove events
		repository.delete(bucket.getId(), Lists.newArrayList(e1.getId(), e2.getId()));
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isZero();
	}

	@Test
	public void testBulkWithNoValidEvents() {
		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket);

		// create event
		Event e1 = new Event(Nodes.newObject("foo", "bar"));
		e1.addValue(Event.TAG, "bad");

		// add event
		assertThatThrownBy(() -> repository.add(bucket.getId(), Lists.newArrayList(e1)))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("is not allowed");
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isEqualTo(0L);
	}

	@Test
	public void testBulkWithInvalidEvent() {
		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket);

		// create events
		Event e1 = new Event();
		e1.addValue(Event.TAG, "good");
		Event e2 = new Event(Nodes.newObject("foo", "bar"));
		e2.addValue(Event.TAG, "bad");

		// add events
		assertThatThrownBy(() -> repository.add(bucket.getId(), Lists.newArrayList(e1, e2)))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("is not allowed");
		repository.refresh(bucket.getId());
		assertThat(repository.size()).as("repository size").isEqualTo(0L);
	}

	@Test
	public void testOptimisticLockFailure() {
		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket);

		Event event = new Event();
		event.setValue(Event.AUTHOR, me);
		event.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		event.addValue(Event.TAG, "original");
		repository.add(bucket.getId(), event);
		repository.refresh(bucket.getId());

		Event current = repository.find(bucket.getId(), event.getId());
		Event stale = current.copy();

		current.addValue(Event.TAG, "updated");
		repository.update(bucket.getId(), current, current);

		stale.addValue(Event.TAG, "conflict");
		assertThatThrownBy(() -> repository.update(bucket.getId(), stale, stale)).hasMessageContaining("409 Conflict");
	}

	@Test
	public void testFields() {
		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket);

		assertThat(repository.fields(bucket.getId())).as("empty bucket").isEmpty();

		// three events set TAG, two set DISTANCE, one sets LOCATION
		Event e1 = new Event();
		e1.setValue(Event.AUTHOR, me);
		e1.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		e1.addValue(Event.TAG, "a");
		e1.setValue(Event.DISTANCE, Measures.valueOf("5 km"));
		e1.setValue(
			Event.LOCATION,
			new Location(new java.math.BigDecimal("37.77"), new java.math.BigDecimal("-122.42"))
		);
		e1.setValue(Event.SOURCE, new Resource("Test", "https://example.com/test"));

		Event e2 = new Event();
		e2.setValue(Event.AUTHOR, me);
		e2.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		e2.addValue(Event.TAG, "b");
		e2.setValue(Event.DISTANCE, Measures.valueOf("3 km"));

		Event e3 = new Event();
		e3.setValue(Event.AUTHOR, me);
		e3.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		e3.addValue(Event.TAG, "c");

		repository.add(bucket.getId(), Lists.newArrayList(e1, e2, e3));
		repository.refresh(bucket.getId());

		// counts: 3 for ID/AUTHOR/TIMESTAMP/TAG, 2 for DISTANCE, 1 for SOURCE/LOCATION;
		// ties broken by Event.FIELDS declaration order
		assertThat(repository.fields(bucket.getId()))
			.as("fields by frequency")
			.containsExactly(
				Event.ID,
				Event.AUTHOR,
				Event.TIMESTAMP,
				Event.TAG,
				Event.DISTANCE,
				Event.SOURCE,
				Event.LOCATION
			);
	}

	@Test
	public void testTimestamp() {
		Bucket bucket = new Bucket();
		new BucketRepository(getManager()).store(bucket);
		Event event = new Event();
		event.setValue(Event.AUTHOR, me);
		event.addValue(Event.TAG, "test");
		event.toJson().put("timestamp", "2012-10-24T13:10:00.000-00:00");
		repository.add(bucket.getId(), event);
	}
}
