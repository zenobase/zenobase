package com.zenobase.search;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.services.ElasticSearchTestSupport;
import com.zenobase.services.Index;

public class SearchTestSupport extends ElasticSearchTestSupport {

	private final EventSearchBuilder search = new EventSearchBuilder();
	private final String bucketId = Generator.id();
	private Index index;

	@Before
	public void setUp() {
		index = getManager().getIndex(bucketId);
		index.create(0);
		index.putMapping(Event.SCHEMA);
		index.refresh();
	}

	protected EventSearchBuilder getSearch() {
		return search;
	}

	protected void addEvent(Event event) {
		addEvent(event, DateTime.now());
	}

	protected void addEvent(Event event, DateTime timestamp) {
		event.prePersist(bucketId);
		index.store(Event.TYPE_NAME, event.getId(), event.toJson(), timestamp, true);
		event.postPersist();
	}

	protected ObjectNode execute() {
		return search.buildSearch().execute(index);
	}
}
