package com.zenobase.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.models.Event;

public class EventSearchBuilder extends SearchBuilderSupport {

	private static final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders = Event.getSchema().getConstraintBuilders();

	private static final FilterParser filterParser = new FilterParser(constraintBuilders);

	private static final ImmutableMap<String, FacetBuilder> facetBuilders = ImmutableMap.<String, FacetBuilder>builder()
		.put(ListFacet.TYPE, ListFacet.builder())
		.put(CountFacet.TYPE, CountFacet.builder())
		.put(GanttFacet.TYPE, GanttFacet.builder())
		.put(RatingsFacet.TYPE, RatingsFacet.builder())
		.put(HistogramFacet.TYPE, HistogramFacet.builder())
		.put(TimelineFacet.TYPE, TimelineFacet.builder(filterParser))
		.put(TimeHistogramFacet.TYPE, TimeHistogramFacet.builder(filterParser))
		.put(MultiplotFacet.TYPE, MultiplotFacet.builder())
		.put(ScoreboardFacet.TYPE, ScoreboardFacet.builder())
		.put(MapFacet.TYPE, MapFacet.builder())
		.put(ScatterPlotFacet.TYPE, ScatterPlotFacet.builder(filterParser))
		.build();

	public EventSearchBuilder() {
		super(constraintBuilders, facetBuilders);
	}
}
