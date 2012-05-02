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

	@Before
	public void setUp() {
		index = new Index(bucketId, getClient());
		index.create(0);
		index.putMapping(Event.getSchema());
	}

	protected void add(Event event) {
		event.prePersist();
		index.store(Event.TYPE_NAME, event.getId(), event.toJson(), true);
		event.setValue(Event.VERSION, null);
	}

	protected ObjectNode execute(String options) {
		EventSearch search = new EventSearch();
		if (!options.isEmpty()) {
			search.addWidget(options);
		}
		ObjectNode result = search.execute(index);
		// System.out.println("r:" + result);
		return result;
	}
}
