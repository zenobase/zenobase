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
		.put(MultiplotWidget.TYPE, MultiplotWidget.builder())
		.put(ScoreboardWidget.TYPE, ScoreboardWidget.builder())
		.put(MapWidget.TYPE, MapWidget.builder())
		.put(CorrelateWidget.TYPE, CorrelateWidget.builder())
		.build();

	private static final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders = Event.getSchema().getConstraintBuilders();

	@Override
	protected ImmutableMap<String, WidgetBuilder> getWidgetBuilders() {
		return widgetBuilders;
	}

	@Override
	protected ImmutableMultimap<String, ConstraintBuilder> getConstraintBuilders() {
		return constraintBuilders;
	}
}
