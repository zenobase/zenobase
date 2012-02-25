package search;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.YearMonth;
import org.joda.time.format.ISODateTimeFormat;

import play.Logger;
import services.IndexManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import common.Intervals;
import common.Nodes;

public class EventSearch {

	private static final ImmutableMap<String, WidgetBuilder> builders = ImmutableMap.<String, WidgetBuilder>builder()
		.put("list", ListWidget.builder())
		.put("count", CountWidget.builder())
		.put("gantt", GanttWidget.builder())
		.put("histogram", HistogramWidget.builder())
		.put("timeline", TimelineWidget.builder())
		.put("scoreboard", ScoreboardWidget.builder())
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
		Matcher m = Pattern.compile("([a-z]+)\\(([^)]+)\\)").matcher(widget);
		Preconditions.checkState(m.matches(), "Invalid widget: %s", widget);
		String type = m.group(1);
		String options = m.group(2);
		widgets.add(builders.get(type).build(WidgetOptions.parse(options)));
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
		if (Event.TIMESTAMP.getName().equals(field)) {
			Interval interval = Intervals.forMonth(YearMonth.parse(value), DateTimeZone.forOffsetHours(-8));
			String from = interval.getStart().toString(ISODateTimeFormat.dateTime());
			String to = interval.getEnd().toString(ISODateTimeFormat.dateTime());
			constraints.add(QueryBuilders.rangeQuery(field).gte(from).lt(to));
		}
		else if (Event.LOCATION.getName().equals(field)) {
			String[] c = value.split("_");
			GeoPoint topLeft = new GeoPoint(Double.parseDouble(c[2]), Double.parseDouble(c[1]));
			GeoPoint bottomRight = new GeoPoint(Double.parseDouble(c[0]), Double.parseDouble(c[3]));
			Logger.info("topLeft: " + toString(topLeft));
			Logger.info("bottomRight: " + toString(bottomRight));
			FilterBuilder locationFilter = FilterBuilders.geoBoundingBoxFilter(field).topLeft(topLeft.getLat(), topLeft.getLon()).bottomRight(bottomRight.getLat(), bottomRight.getLon()).type("memory");
			constraints.add(QueryBuilders.constantScoreQuery(locationFilter));
		}
		else {
			constraints.add(QueryBuilders.termQuery(field, value));
		}
		return this;
	}

	public static String toString(GeoPoint point) {
		return String.format("%.4f,%.4f", point.getLat(), point.getLon());
	}

	public ObjectNode execute(IndexManager index) {
		SearchSourceBuilder builder = buildSearch();
		Logger.info("q: " + builder);
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
