package com.zenobase.search.facets;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;

/**
 * Bucket helpers that hide whether OpenSearch returned a string-terms or long-terms aggregation.
 *
 * <p>OpenSearch chooses the variant based on the underlying field mapping (keyword → {@code sterms},
 * long → {@code lterms}). Callers can use {@link #buckets(Aggregate)} and read {@link Bucket#key()}
 * uniformly as a string label.
 */
final class TermsBuckets {

	record Bucket(String key, long docCount, Map<String, Aggregate> aggregations) {}

	private TermsBuckets() {}

	static List<Bucket> buckets(Aggregate aggregate) {
		if (aggregate.isSterms()) {
			return aggregate
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(b -> new Bucket(b.key(), b.docCount(), b.aggregations()))
				.toList();
		}
		if (aggregate.isLterms()) {
			return aggregate
				.lterms()
				.buckets()
				.array()
				.stream()
				.map(b -> new Bucket(b.key()._toJsonString(), b.docCount(), b.aggregations()))
				.toList();
		}
		throw new IllegalArgumentException("Unsupported terms aggregate variant: " + aggregate._kind());
	}

	static @Nullable Long sumOtherDocCount(Aggregate aggregate) {
		if (aggregate.isSterms()) {
			return aggregate.sterms().sumOtherDocCount();
		}
		if (aggregate.isLterms()) {
			return aggregate.lterms().sumOtherDocCount();
		}
		throw new IllegalArgumentException("Unsupported terms aggregate variant: " + aggregate._kind());
	}
}
