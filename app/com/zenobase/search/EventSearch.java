package com.zenobase.search;

import java.util.List;
import java.util.Set;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import com.zenobase.json.IntegerField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.services.Index;

public class EventSearch {

	public static final IntegerField TOTAL = new IntegerField("total");

	private static final ImmutableMap<String, WidgetBuilder> widgetBuilders = ImmutableMap.<String, WidgetBuilder>builder()
		.put(ListWidget.TYPE, ListWidget.builder())
		.put(CountWidget.TYPE, CountWidget.builder())
		.put(GanttWidget.TYPE, GanttWidget.builder())
		.put(HistogramWidget.TYPE, HistogramWidget.builder())
		.put(TimelineWidget.TYPE, TimelineWidget.builder())
		.put(PlotWidget.TYPE, PlotWidget.builder())
		.put(ScoreboardWidget.TYPE, ScoreboardWidget.builder())
		.build();

	private final ImmutableMultimap<String, Constraint> constraintBuilders = ImmutableMultimap.<String, Constraint>builder()
		.put(Event.TAG.getName(), new TermConstraint())
		.put(Event.AUTHOR.getName(), new TermConstraint())
		.put(Event.TIMESTAMP.getName(), new DateTimeRangeConstraint())
		.put(Event.LOCATION.getName(), new BoundingBoxConstraint())
		.build();

	private final Set<Widget> widgets = Sets.newLinkedHashSet();
	private final List<QueryBuilder> constraints = Lists.newArrayList();

	public EventSearch addWidgets(String[] widgets) {
		if (widgets != null) {
			for (String widget : widgets) {
				addWidget(widget);
			}
		}
		return this;
	}

	public EventSearch addWidget(String widget) {
		WidgetOptions options = WidgetOptions.parse(widget);
		return addWidget(widgetBuilders.get(options.get("type")).build(options));
	}

	public EventSearch addWidget(Widget widget) {
		widgets.add(widget);
		return this;
	}

	public EventSearch addFilters(String[] filters) {
		if (filters != null) {
			for (String filter : filters) {
				addFilter(filter);
			}
		}
		return this;
	}

	public EventSearch addFilter(String expression) {
		String[] tokens = expression.split(":", 2);
		String field = tokens[0];
		String value = tokens[1];
		for (Constraint constraint : constraintBuilders.get(field)) {
			QueryBuilder builder = constraint.build(field, value);
			if (builder != null) {
				constraints.add(builder);
				return this;
			}
		}
		throw new IllegalArgumentException("Don't know what to do with filter: " + expression);
	}

	public ObjectNode execute(Index index) {
		SearchSourceBuilder builder = buildSearch();
		// Logger.info("q: " + builder);
		SearchResponse response = index.search(builder);
		// Logger.info("r: " + response);
		return toJson(response);
	}

	private SearchSourceBuilder buildSearch() {
		SearchSourceBuilder builder = new SearchSourceBuilder().query(buildQuery());
		for (Widget widget : widgets) {
			widget.configure(builder);
		}
		return builder;
	}

	private QueryBuilder buildQuery() {
		QueryBuilder query = null;
		if (constraints.isEmpty()) {
			query = QueryBuilders.matchAllQuery();
		} else {
			query = QueryBuilders.boolQuery();
			for (QueryBuilder constraint : constraints) {
				((BoolQueryBuilder) query).must(constraint);
			}
		}
		return query;
	}

	private ObjectNode toJson(SearchResponse response) {
		ObjectNode node = Nodes.newObject();
		TOTAL.setValue(node, Ints.checkedCast(response.hits().getTotalHits()));
		for (Widget widget : widgets) {
			node.put(widget.getId(), widget.process(response));
		}
		return node;
	}

	@Override
	public String toString() {
		return String.format("EventSearch(widgets:%s, constraints:%s", widgets, constraints);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof EventSearch &&
			equals((EventSearch) that);
	}

	private boolean equals(EventSearch that) {
		return widgets.toString().equals(that.widgets.toString()) &&
			constraints.toString().equals(that.constraints.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(widgets.toString(), constraints.toString());
	}
}
