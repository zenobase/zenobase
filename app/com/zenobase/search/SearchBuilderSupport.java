package com.zenobase.search;

import java.util.List;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilder;
import play.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class SearchBuilderSupport {

	private final Set<Facet> facets = Sets.newLinkedHashSet();
	private final List<QueryBuilder> must = Lists.newArrayList();
	private final List<QueryBuilder> mustNot = Lists.newArrayList();

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
		FacetBuilder builder = getFacetBuilders().get(type);
		if (builder == null) {
			Logger.warn("Facet builder not registered: " + type);
			return this;
		}
		return addWidget(builder.build(options));
	}

	public SearchBuilderSupport addWidget(Facet facet) {
		facets.add(facet);
		return this;
	}

	public SearchBuilderSupport addConstraints(String[] expressions) {
		if (expressions != null) {
			for (String expression : expressions) {
				addConstraint(expression);
			}
		}
		return this;
	}

	public SearchBuilderSupport addConstraint(String expression) {
		String[] tokens = expression.split(":", 2);
		String field = tokens[0];
		String value = tokens[1];
		boolean negated = false;
		if (field.startsWith("-")) {
			negated = true;
			field = field.substring(1);
		}
		for (ConstraintBuilder constraint : getConstraintBuilders().get(field)) {
			QueryBuilder builder = constraint.build(field, value);
			if (builder != null) {
				return addConstraint(builder, negated);
			}
		}
		throw new IllegalArgumentException("Don't know what to do with constraint: " + expression);
	}

	private SearchBuilderSupport addConstraint(QueryBuilder builder, boolean negated) {
		(negated ? mustNot : must).add(builder);
		return this;
	}

	protected abstract ImmutableMap<String, FacetBuilder> getFacetBuilders();

	protected abstract ImmutableMultimap<String, ConstraintBuilder> getConstraintBuilders();

	public Search build() {
		return new Search(facets, must, mustNot);
	}
}
