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
		.put(MapFacet.TYPE, MapFacet.builder())
		.put(HeatmapFacet.TYPE, HeatmapFacet.builder())
		.put(RatingsFacet.TYPE, RatingsFacet.builder(filterParser))
		.put(HistogramFacet.TYPE, HistogramFacet.builder(filterParser))
		.put(TimelineFacet.TYPE, TimelineFacet.builder(filterParser))
		.put(PolarFacet.TYPE, PolarFacet.builder(filterParser))
		.put(ScoreboardFacet.TYPE, ScoreboardFacet.builder(filterParser))
		.put(ScatterPlotFacet.TYPE, ScatterPlotFacet.builder(filterParser))
		.put(StatsFacet.TYPE, StatsFacet.builder(filterParser))
		.put(MultiplotFacet.TYPE, MultiplotFacet.builder())
		.build();

	public EventSearchBuilder() {
		super(constraintBuilders, facetBuilders);
	}
}
