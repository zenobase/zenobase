package com.zenobase.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.models.Event;

public class EventSearchBuilder extends SearchBuilderSupport {

	private static final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders = Event.SCHEMA.getConstraintBuilders();
	private static final FilterParser filterParser = new FilterParser(constraintBuilders);

	private static final ImmutableMap<String, FacetBuilder> facetBuilders = ImmutableMap.<String, FacetBuilder>builder()
		.put(ListFacet.TYPE, ListFacet.builder(filterParser, Event.SCHEMA))
		.put(CountFacet.TYPE, CountFacet.builder(filterParser))
		.put(GanttFacet.TYPE, GanttFacet.builder(filterParser))
		.put(MapFacet.TYPE, MapFacet.builder())
		.put(HeatmapFacet.TYPE, HeatmapFacet.builder(filterParser))
		.put(GeoBoundsFacet.TYPE, GeoBoundsFacet.builder(filterParser))
		.put(RatingsFacet.TYPE, RatingsFacet.builder(filterParser))
		.put(HistogramFacet.TYPE, HistogramFacet.builder(filterParser))
		.put(TimelineFacet.TYPE, TimelineFacet.builder(filterParser))
		.put(PolarFacet.TYPE, PolarFacet.builder(filterParser))
		.put(ScoreboardFacet.TYPE, ScoreboardFacet.builder(filterParser))
		.put(ScatterPlotFacet.TYPE, ScatterPlotFacet.builder(filterParser))
		.put(StatsFacet.TYPE, StatsFacet.builder(filterParser))
		.build();

	public EventSearchBuilder() {
		super(constraintBuilders, facetBuilders);
	}
}
