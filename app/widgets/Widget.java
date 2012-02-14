package widgets;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

import com.google.common.collect.Multiset;

public interface Widget {

	void configure(SearchRequestBuilder request);

	Multiset<?> getResult(SearchResponse response);
}
