package widgets;

import org.codehaus.jackson.JsonNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public interface Widget {

	String getId();

	void configure(SearchSourceBuilder request);

	JsonNode process(SearchResponse response);
}
