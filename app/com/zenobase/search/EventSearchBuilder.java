package com.zenobase.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.models.Event;

public class EventSearchBuilder extends SearchBuilderSupport {

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

	@Override
	protected ImmutableMap<String, WidgetBuilder> getWidgetBuilders() {
		return widgetBuilders;
	}

	@Override
	protected ImmutableMultimap<String, Constraint> getConstraintBuilders() {
		return constraintBuilders;
	}
}
