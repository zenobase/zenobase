package com.zenobase.search.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

public class GeoBoundingBoxTest {

	private static Point point(double lon, double lat) {
		return SpatialContext.GEO.getShapeFactory().pointXY(lon, lat);
	}

	@Test
	public void testAcceptsWellFormedCorners() {
		GeoBoundingBox box = new GeoBoundingBox(point(-10, 10), point(10, -10));
		assertThat(box.topLeft().getY()).isEqualTo(10.0);
		assertThat(box.bottomRight().getX()).isEqualTo(10.0);
	}

	@Test
	public void testRejectsTopLeftBelowBottomRight() {
		assertThatThrownBy(() -> new GeoBoundingBox(point(-10, -10), point(10, 10))).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	public void testRejectsTopLeftEastOfBottomRight() {
		assertThatThrownBy(() -> new GeoBoundingBox(point(10, 10), point(-10, -10))).isInstanceOf(
			IllegalArgumentException.class
		);
	}
}
