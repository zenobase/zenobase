package com.zenobase.search;

import java.util.List;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilder;
import play.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.zenobase.models.Event;

public class EventSearchBuilder {

	private static final ImmutableMap<String, WidgetBuilder> widgetBuilders = ImmutableMap.<String, WidgetBuilder>builder()
		.put(ListWidget.TYPE, ListWidget.builder())
		.put(CountWidget.TYPE, CountWidget.builder())
		.put(GanttWidget.TYPE, GanttWidget.builder())
		.put(RatingsWidget.TYPE, RatingsWidget.builder())
		.put(HistogramWidget.TYPE, HistogramWidget.builder())
		.put(TimelineWidget.TYPE, TimelineWidget.builder())
		.put(TimeHistogramWidget.TYPE, TimeHistogramWidget.builder())
		.put(PlotWidget.TYPE, PlotWidget.builder())
		.put(ScoreboardWidget.TYPE, ScoreboardWidget.builder())
		.put(MapWidget.TYPE, MapWidget.builder())
		.build();

	private static final ImmutableMultimap<String, Constraint> constraintBuilders = Event.getConstraints();

	private final Set<Widget> widgets = Sets.newLinkedHashSet();
	private final List<QueryBuilder> constraints = Lists.newArrayList();

	public EventSearchBuilder addWidgets(String[] widgets) {
		if (widgets != null) {
			for (String widget : widgets) {
				addWidget(widget);
			}
		}
		return this;
	}

	public EventSearchBuilder addWidget(String widget) {
		WidgetOptions options = WidgetOptions.parse(widget);
		String type = options.get("type");
		WidgetBuilder builder = widgetBuilders.get(type);
		if (builder == null) {
			Logger.warn("Widget builder not registered: " + type);
			return this;
		}
		return addWidget(builder.build(options));
	}

	public EventSearchBuilder addWidget(Widget widget) {
		widgets.add(widget);
		return this;
	}

	public EventSearchBuilder addConstraints(String[] expressions) {
		if (expressions != null) {
			for (String expression : expressions) {
				addConstraint(expression);
			}
		}
		return this;
	}

	public EventSearchBuilder addConstraint(String expression) {
		String[] tokens = expression.split(":", 2);
		String field = tokens[0];
		String value = tokens[1];
		for (Constraint constraint : constraintBuilders.get(field)) {
			QueryBuilder builder = constraint.build(field, value);
			if (builder != null) {
				return addConstraint(builder);
			}
		}
		throw new IllegalArgumentException("Don't know what to do with constraint: " + expression);
	}

	public EventSearchBuilder addConstraint(QueryBuilder builder) {
		constraints.add(builder);
		return this;
	}

	public Search build() {
		return new Search(widgets, constraints);
	}
}
