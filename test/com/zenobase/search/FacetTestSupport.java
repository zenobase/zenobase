package com.zenobase.search;

import com.zenobase.common.Generator;

public class FacetTestSupport extends SearchTestSupport {

	protected static final String FACET_ID = Generator.id();

	protected void addFacet(String options, Object... args) {
		getSearch().addFacet(String.format(options, args));
	}
}
