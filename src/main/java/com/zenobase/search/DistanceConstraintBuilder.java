package com.zenobase.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.opensearch.client.opensearch._types.query_dsl.GeoDistanceQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.GeoLocation;
import org.opensearch.client.opensearch._types.LatLonGeoLocation;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Location;

public class DistanceConstraintBuilder extends ConstraintBuilder {

	private static final Pattern PATTERN = Pattern.compile("([^,]+),([^~]+)(?:~(.+))?");
	private static final String DEFAULT_DISTANCE = "1 m"; // 0 won't match

	public DistanceConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		Matcher m = PATTERN.matcher(value);
		return m.matches() ? build(extractLocation(m), extractDistance(m)) : null;
	}

	private Location extractLocation(Matcher m) {
		return new Location(m.group(1), m.group(2));
	}

	private DecimalMeasure<Length> extractDistance(Matcher m) {
		String value = MoreObjects.firstNonNull(m.group(3), DEFAULT_DISTANCE);
		return Measures.valueOf(value);
	}

	private Query build(Location location, DecimalMeasure<Length> distance) {
		double lat = location.getLatitude().doubleValue();
		double lon = location.getLongitude().doubleValue();
		String dist = distance.doubleValue(Units.KM) + "km";
		return Query.of(q -> q.geoDistance(g -> g
			.field(getPath())
			.location(GeoLocation.of(gl -> gl.latlon(LatLonGeoLocation.of(ll -> ll.lat(lat).lon(lon)))))
			.distance(dist)
		));
	}
}
