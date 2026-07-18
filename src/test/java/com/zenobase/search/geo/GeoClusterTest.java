package com.zenobase.search.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

public class GeoClusterTest {

	private static Point point(double lon, double lat) {
		return SpatialContext.GEO.getShapeFactory().pointXY(lon, lat);
	}

	@Test
	public void testMergeSumsCounts() {
		GeoCluster left = new GeoCluster(2, "9q8y", point(0, 0));
		GeoCluster right = new GeoCluster(3, "9q8y", point(0, 0));
		assertThat(left.merge(right).count()).isEqualTo(5);
	}

	@Test
	public void testMergeComputesWeightedMeanCenter() {
		GeoCluster left = new GeoCluster(2, "9q8y", point(10, 20));
		GeoCluster right = new GeoCluster(3, "9q8y", point(0, 0));

		GeoCluster merged = left.merge(right);
		// weights are left.count (2) and right.count (3)
		assertThat(merged.center().getX()).isEqualTo((10.0 * 2 + 0.0 * 3) / 5); // lon = 4
		assertThat(merged.center().getY()).isEqualTo((20.0 * 2 + 0.0 * 3) / 5); // lat = 8
	}

	@Test
	public void testMergeKeepsLeftGeohash() {
		GeoCluster left = new GeoCluster(1, "9q8y", point(0, 0));
		GeoCluster right = new GeoCluster(1, "9q8z", point(0, 0));
		assertThat(left.merge(right).geohash()).isEqualTo("9q8y");
	}

	@Test
	public void testBoundsSurroundGeohashCell() {
		GeoCluster cluster = new GeoCluster(1, "9q8yy", point(-122.4, 37.7));
		GeoBoundingBox bounds = cluster.bounds();

		// top-left is north-west, bottom-right is south-east
		assertThat(bounds.topLeft().getY()).isGreaterThan(bounds.bottomRight().getY());
		assertThat(bounds.topLeft().getX()).isLessThan(bounds.bottomRight().getX());
	}
}
