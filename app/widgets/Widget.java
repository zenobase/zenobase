package widgets;

import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

public interface Widget {

	void configure(SearchRequestBuilder request);

	Iterable<?> getResult(SearchResponse response);
}
