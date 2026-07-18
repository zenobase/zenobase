package com.zenobase.search.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

public class GeoClusterBuilderTest {

	private static Point point(double lon, double lat) {
		return SpatialContext.GEO.getShapeFactory().pointXY(lon, lat);
	}

	@Test
	public void testBuildEmptyByDefault() {
		assertThat(new GeoClusterBuilder().build()).isEmpty();
	}

	@Test
	public void testAddReducesGeohashPrecision() {
		GeoClusterBuilder builder = new GeoClusterBuilder();
		builder.add(1, "9q8yy", point(0, 0));
		assertThat(builder.build()).singleElement().extracting(GeoCluster::geohash).isEqualTo("9q8y");
	}

	@Test
	public void testAddMergesClustersSharingReducedGeohash() {
		GeoClusterBuilder builder = new GeoClusterBuilder();
		builder.add(2, "9q8ya", point(0, 0));
		builder.add(3, "9q8yb", point(0, 0));

		assertThat(builder.build())
			.singleElement()
			.satisfies(cluster -> {
				assertThat(cluster.geohash()).isEqualTo("9q8y");
				assertThat(cluster.count()).isEqualTo(5);
			});
	}

	@Test
	public void testAddKeepsDistinctGeohashesSeparate() {
		GeoClusterBuilder builder = new GeoClusterBuilder();
		builder.add(1, "9q8ya", point(0, 0));
		builder.add(1, "dr5rk", point(0, 0));
		assertThat(builder.build()).hasSize(2);
	}

	@Test
	public void testAddHandlesEmptyGeohash() {
		GeoClusterBuilder builder = new GeoClusterBuilder();
		builder.add(1, "", point(0, 0));
		assertThat(builder.build()).singleElement().extracting(GeoCluster::geohash).isEqualTo("");
	}
}
