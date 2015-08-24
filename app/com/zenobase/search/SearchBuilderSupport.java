package com.zenobase.search;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import org.elasticsearch.index.query.QueryBuilder;
import play.Logger;

public class SearchBuilderSupport extends FilterBuilderSupport {

	private final ImmutableMap<String, FacetBuilder> facetBuilders;
	private final Set<Facet> facets = Sets.newLinkedHashSet();

	public SearchBuilderSupport(ImmutableMultimap<String, ConstraintBuilder> constraintBuilders, ImmutableMap<String, FacetBuilder> facetBuilders) {
		super(constraintBuilders);
		this.facetBuilders = facetBuilders;
	}

	@Override
	public SearchBuilderSupport addConstraint(String expression) {
		super.addConstraint(expression);
		return this;
	}

	@Override
	public SearchBuilderSupport addConstraint(QueryBuilder builder, boolean negated) {
		super.addConstraint(builder, negated);
		return this;
	}

	@Override
	public SearchBuilderSupport addConstraints(Iterable<String> expressions) {
		super.addConstraints(expressions);
		return this;
	}

	public SearchBuilderSupport addFacets(String[] facets) {
		if (facets != null) {
			for (String facet : facets) {
				addFacet(facet);
			}
		}
		return this;
	}

	public SearchBuilderSupport addFacets(Iterable<FacetOptions> options) {
		if (options != null) {
			for (FacetOptions option : options) {
				addFacet(option);
			}
		}
		return this;
	}

	public SearchBuilderSupport addFacet(String options) {
		return addFacet(FacetOptions.parse(options));
	}

	public SearchBuilderSupport addFacet(FacetOptions options) {
		String type = options.get("type");
		FacetBuilder builder = facetBuilders.get(type);
		if (builder == null) {
			Logger.warn("Facet builder not registered: {}", type);
			return this;
		}
		return addFacet(builder.build(options));
	}

	public SearchBuilderSupport addFacet(Facet facet) {
		facets.add(facet);
		return this;
	}

	public Search buildSearch() {
		return new Search(facets, getMust(), getMustNot());
	}
}
