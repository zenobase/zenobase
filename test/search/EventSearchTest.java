package search;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

public class EventSearchTest {

	@Test
	public void test() {
		FilterBuilder filter = FilterBuilders.geoBoundingBoxFilter("pin.location").topLeft(40.73, -74.1).bottomRight(40.717, -73.99);
		System.out.println(new SearchSourceBuilder().filter(filter).toString());
	}
}
