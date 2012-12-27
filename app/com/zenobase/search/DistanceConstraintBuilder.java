package com.zenobase.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import com.google.common.base.Objects;

import com.zenobase.common.Measures;
import com.zenobase.models.Location;

public class DistanceConstraintBuilder implements ConstraintBuilder {

	private static final Pattern PATTERN = Pattern.compile("([^,]+),([^~]+)(?:~(.+))?");
	private static final String DEFAULT_DISTANCE = "1 m"; // 0 won't match

	@Override
	public QueryBuilder build(String field, String value) {
		Matcher m = PATTERN.matcher(value);
		return m.matches() ? build(field, extractLocation(m), extractDistance(m)) : null;
	}

	private Location extractLocation(Matcher m) {
		return new Location(m.group(1), m.group(2));
	}

	private DecimalMeasure<Length> extractDistance(Matcher m) {
		String value = Objects.firstNonNull(m.group(3), DEFAULT_DISTANCE);
		return Measures.<Length>valueOf(value);
	}

	private QueryBuilder build(String field, Location location, DecimalMeasure<Length> distance) {
		FilterBuilder filter = FilterBuilders.geoDistanceFilter(field)
			.lat(location.getLatitude().doubleValue())
			.lon(location.getLongitude().doubleValue())
			.distance(distance.doubleValue(SI.KILOMETER), DistanceUnit.KILOMETERS);
		return QueryBuilders.constantScoreQuery(filter);
	}
}
