package com.zenobase.search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ListWidget implements Widget {

	private final String id;
	private final int offset;
	private final int limit;
	private final String sort;
	private final SortOrder order;

	private ListWidget(String id, int offset, int limit, String sort, SortOrder order) {
		this.id = id;
		this.offset = offset;
		this.limit = limit;
		this.sort = sort;
		this.order = order;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.from(offset);
		builder.size(limit);
		builder.sort(sort, order);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode eventsNode = Nodes.newArray();
		for (SearchHit hit : response.hits()) {
			Event event = new Event(Nodes.read(hit.source()));
			eventsNode.add(event.toJson());
		}
		return eventsNode;
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new ListWidget(
					options.get("id"),
					options.get("offset", Integer.class, 0),
					options.get("limit", Integer.class, 10),
					options.get("order", String.class, "dateTime"),
					options.get("reverse", Boolean.class, Boolean.FALSE) ? SortOrder.ASC : SortOrder.DESC);
			}
		};
	}
}
