package search;

import org.elasticsearch.index.query.QueryBuilder;

public interface Constraint {

	QueryBuilder build(String field, String value);
}
