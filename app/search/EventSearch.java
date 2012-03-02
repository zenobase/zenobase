package search;

import java.util.List;
import java.util.Set;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import services.IndexManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import common.Nodes;

public class EventSearch {

	private static final ImmutableMap<String, WidgetBuilder> widgetBuilders = ImmutableMap.<String, WidgetBuilder>builder()
		.put("list", ListWidget.builder())
		.put("count", CountWidget.builder())
		.put("gantt", GanttWidget.builder())
		.put("histogram", HistogramWidget.builder())
		.put("timeline", TimelineWidget.builder())
		.put("scoreboard", ScoreboardWidget.builder())
		.build();

	private final ImmutableMultimap<String, Constraint> constraintBuilders = ImmutableMultimap.<String, Constraint>builder()
		.put(Event.TAG.getName(), new TermConstraint())
		.put(Event.TIMESTAMP.getName(), new RangeConstraint())
		.put(Event.LOCATION.getName(), new BoundingBoxConstraint())
		.build();

	private final Bucket bucket;
	private final Set<Widget> widgets = Sets.newLinkedHashSet();
	private final List<QueryBuilder> constraints = Lists.newArrayList();

	public EventSearch(Bucket bucket) {
		this.bucket = bucket;
	}

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
		widgets.add(widgetBuilders.get(options.get("type")).build(options));
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

	public EventSearch addFilter(String filter) {
		String[] tokens = filter.split(":", 2);
		String field = tokens[0];
		String value = tokens[1];
		for (Constraint constraint : constraintBuilders.get(field)) {
			QueryBuilder builder = constraint.build(field, value);
			if (builder != null) {
				constraints.add(builder);
				return this;
			}
		}
		throw new IllegalArgumentException("Don't know whet to do with filter: " + filter);
	}

	public ObjectNode execute(IndexManager index) {
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
		ObjectNode object = Nodes.newObject();
		object.putAll(bucket.toJson());
		object.put("total", Ints.checkedCast(response.hits().getTotalHits()));
		for (Widget widget : widgets) {
			object.put(widget.getId(), widget.process(response));
		}
		return object;
	}
}
