package com.zenobase.search;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.services.ElasticSearchTestSupport;
import com.zenobase.services.Index;

public class SearchTestSupport extends ElasticSearchTestSupport {

	private final EventSearchBuilder search = new EventSearchBuilder();
	private Index index;

	@Before
	public void setUp() {
		index = getManager().getIndex(Generator.id());
		index.create(0);
		index.putMapping(Event.getSchema());
		index.refresh();
	}

	protected EventSearchBuilder getSearch() {
		return search;
	}

	protected void addEvent(Event event) {
		event.prePersist();
		index.store(Event.TYPE_NAME, event.getId(), event.toJson(), true);
	}

	protected ObjectNode execute() {
		return search.build().execute(index);
	}
}
