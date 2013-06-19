package com.zenobase.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.models.Event;

public class EventSearchBuilder extends SearchBuilderSupport {

	private static final ImmutableMap<String, FacetBuilder> facetBuilders = ImmutableMap.<String, FacetBuilder>builder()
		.put(ListFacet.TYPE, ListFacet.builder())
		.put(CountFacet.TYPE, CountFacet.builder())
		.put(GanttFacet.TYPE, GanttFacet.builder())
		.put(RatingsFacet.TYPE, RatingsFacet.builder())
		.put(HistogramFacet.TYPE, HistogramFacet.builder())
		.put(TimelineFacet.TYPE, TimelineFacet.builder())
		.put(TimeHistogramWidget.TYPE, TimeHistogramWidget.builder())
		.put(MultiplotFacet.TYPE, MultiplotFacet.builder())
		.put(ScoreboardFacet.TYPE, ScoreboardFacet.builder())
		.put(MapFacet.TYPE, MapFacet.builder())
		.put(ScatterPlotWidget.TYPE, ScatterPlotWidget.builder())
		.build();

	private static final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders = Event.getSchema().getConstraintBuilders();

	@Override
	protected ImmutableMap<String, FacetBuilder> getFacetBuilders() {
		return facetBuilders;
	}

	@Override
	protected ImmutableMultimap<String, ConstraintBuilder> getConstraintBuilders() {
		return constraintBuilders;
	}
}
