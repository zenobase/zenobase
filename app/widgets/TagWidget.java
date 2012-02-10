package widgets;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public class TagWidget implements Widget {

	private final AbstractFacetBuilder facet;

	public TagWidget(String field, int limit) {
		facet = FacetBuilders.termsFacet(getClass().getName()).field(field).size(limit);
	}

	@Override
	public void configure(SearchRequestBuilder request) {
		request.addFacet(facet);
	}

	@Override
	public Multiset<String> getResult(SearchResponse response) {
		Multiset<String> result = LinkedHashMultiset.create();
		TermsFacet terms = response.facets().facet(TermsFacet.class, getClass().getName());
		for (TermsFacet.Entry tag : terms.entries()) {
			result.add(tag.getTerm(), tag.getCount());
		}
		return result;
	}
}
