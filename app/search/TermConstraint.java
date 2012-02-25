package search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class TermConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		return QueryBuilders.termQuery(field, value);
	}
}
