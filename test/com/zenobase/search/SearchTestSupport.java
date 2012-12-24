package com.zenobase.search;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.services.ElasticSearchTestSupport;
import com.zenobase.services.Index;

public class SearchTestSupport extends ElasticSearchTestSupport {

	private final String bucketId = Generator.id();
	private Index index;
	private EventSearch search = new EventSearch();

	@Before
	public void setUp() {
		index = getManager().getIndex(bucketId);
		index.create(0);
		index.putMapping(Event.getSchema());
		index.refresh();
	}

	protected void addEvent(Event event) {
		event.prePersist();
		index.store(Event.TYPE_NAME, event.getId(), event.toJson(), true);
	}

	protected void addWidget(String options, Object... args) {
		search.addWidget(String.format(options, args));
	}

	protected void addFilter(String expression, Object... args) {
		search.addFilter(String.format(expression, args));
	}

	protected ObjectNode execute() {
		return search.execute(index);
	}
}
