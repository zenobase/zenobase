package com.zenobase.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.models.Event;
import com.zenobase.search.constraints.ConstraintBuilder;
import com.zenobase.search.constraints.FilterParser;
import com.zenobase.search.facets.CountFacet;
import com.zenobase.search.facets.FacetBuilder;
import com.zenobase.search.facets.GanttFacet;
import com.zenobase.search.facets.GeoBoundsFacet;
import com.zenobase.search.facets.HeatmapFacet;
import com.zenobase.search.facets.HistogramFacet;
import com.zenobase.search.facets.ListFacet;
import com.zenobase.search.facets.MapFacet;
import com.zenobase.search.facets.PolarFacet;
import com.zenobase.search.facets.RatingsFacet;
import com.zenobase.search.facets.ScatterPlotFacet;
import com.zenobase.search.facets.ScoreboardFacet;
import com.zenobase.search.facets.StatsFacet;
import com.zenobase.search.facets.TimelineFacet;

public class EventSearchBuilder extends SearchBuilderSupport {

	private static final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders =
			Event.SCHEMA.getConstraintBuilders();
	private static final FilterParser filterParser = new FilterParser(constraintBuilders);

	private static final ImmutableMap<String, FacetBuilder> facetBuilders = ImmutableMap.<String, FacetBuilder>builder()
			.put(ListFacet.TYPE, ListFacet.builder(filterParser, Event.SCHEMA))
			.put(CountFacet.TYPE, CountFacet.builder(filterParser))
			.put(GanttFacet.TYPE, GanttFacet.builder(filterParser))
			.put(MapFacet.TYPE, MapFacet.builder(filterParser))
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
