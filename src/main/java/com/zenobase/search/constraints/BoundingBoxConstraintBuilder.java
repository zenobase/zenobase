package com.zenobase.search.constraints;

import com.google.common.base.Splitter;
import com.zenobase.models.Location;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.GeoBounds;
import org.opensearch.client.opensearch._types.GeoLocation;
import org.opensearch.client.opensearch._types.LatLonGeoLocation;
import org.opensearch.client.opensearch._types.TopLeftBottomRightGeoBounds;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class BoundingBoxConstraintBuilder extends ConstraintBuilder {

	public BoundingBoxConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		List<String> tokens = Splitter.on(',').splitToList(value);
		return tokens.size() == 4
			? build(new Location(tokens.get(2), tokens.get(1)), new Location(tokens.get(0), tokens.get(3)))
			: null;
	}

	private Query build(Location topLeft, Location bottomRight) {
		double tlLat = topLeft.latitude().doubleValue();
		double tlLon = topLeft.longitude().doubleValue();
		double brLat = bottomRight.latitude().doubleValue();
		double brLon = bottomRight.longitude().doubleValue();
		return Query.of(q ->
			q.geoBoundingBox(g ->
				g
					.field(getPath())
					.boundingBox(
						GeoBounds.of(gb ->
							gb.tlbr(
								TopLeftBottomRightGeoBounds.of(tlbr ->
									tlbr
										.topLeft(
											GeoLocation.of(gl ->
												gl.latlon(LatLonGeoLocation.of(ll -> ll.lat(tlLat).lon(tlLon)))
											)
										)
										.bottomRight(
											GeoLocation.of(gl ->
												gl.latlon(LatLonGeoLocation.of(ll -> ll.lat(brLat).lon(brLon)))
											)
										)
								)
							)
						)
					)
			)
		);
	}
}
