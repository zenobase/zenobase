package widgets;

import org.codehaus.jackson.JsonNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

public interface Widget {

	String getId();

	void configure(SearchRequestBuilder request);

	JsonNode process(SearchResponse response);
}
