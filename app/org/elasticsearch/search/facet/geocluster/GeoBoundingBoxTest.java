package org.elasticsearch.search.facet.geocluster;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.junit.Test;

public class GeoBoundingBoxTest {
	
	private final GeoBoundingBox colorado = new GeoBoundingBox(new GeoPoint(41.00, -109.05), new GeoPoint(37.00, -102.04));
	private final GeoPoint denver = new GeoPoint(39.75, -104.87);
	private final GeoPoint lasvegas = new GeoPoint(36.08, -115.17);
	private final GeoPoint sandiego = new GeoPoint(32.82, -117.13);

	@Test
	public void testSize() {
		Assert.assertEquals("Size of Denver", 0.0, new GeoBoundingBox(denver, denver).size(DistanceUnit.KILOMETERS));
		Assert.assertTrue("Colorado is big", colorado.size(DistanceUnit.KILOMETERS) > 750.0);
	}

	@Test
	public void testContains() {
		Assert.assertTrue("Top left corner is in bounds", colorado.contains(colorado.topLeft()));
		Assert.assertTrue("Bottom right corner is in bounds", colorado.contains(colorado.bottomRight()));
		Assert.assertTrue("Denver is in Colorado", colorado.contains(denver));
		Assert.assertFalse("Las Vegas is in Colorado", colorado.contains(lasvegas));
	}

	@Test
	public void testExtend() {
		GeoBoundingBox southwest = colorado.extend(sandiego);
		Assert.assertTrue("Denver is in the South West", southwest.contains(denver));
		Assert.assertTrue("Las Vegas is in the South West", southwest.contains(lasvegas));
	}
}
