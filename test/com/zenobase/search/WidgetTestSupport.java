package com.zenobase.search;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.services.ElasticSearchTestSupport;
import com.zenobase.services.Index;

public class WidgetTestSupport extends ElasticSearchTestSupport {

	private final String bucketId = Generator.id();
	private Index index;
	private EventSearch search = new EventSearch();

	@Before
	public void setUp() {
		index = new Index(bucketId, getClient());
		index.create(0);
		index.putMapping(Event.getSchema());
	}

	protected void addEvent(Event event) {
		event.prePersist();
		index.store(Event.TYPE_NAME, event.getId(), event.toJson(), true);
		event.setValue(Event.VERSION, null);
	}

	protected void addWidget(String options) {
		search.addWidget(options);
	}

	protected ObjectNode execute() {
		ObjectNode result = search.execute(index);
		// System.out.println("r:" + result);
		return result;
	}
}
