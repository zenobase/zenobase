package org.elasticsearch.search.facet.geocluster;

import junit.framework.Assert;

import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.junit.Test;

public class GeoClusterTest {

	private final GeoPoint denver = new GeoPoint(39.75, -104.87);
	private final GeoPoint lasvegas = new GeoPoint(36.08, -115.17);
	private final GeoPoint sandiego = new GeoPoint(32.82, -117.13);

	@Test
	public void test() {

		GeoCluster cluster = new GeoCluster();
		Assert.assertEquals(0, cluster.size());
		Assert.assertNull(cluster.center());
		Assert.assertNull(cluster.bounds());

		cluster.add(denver);
		Assert.assertEquals("Cluster size after adding the first point", 1, cluster.size());
		assertEquals("Center after adding the first point", denver, cluster.center());
		assertEquals("Top left corner after adding the first point", denver, cluster.bounds().topLeft());
		assertEquals("Bottom right corner after adding the first point", denver, cluster.bounds().bottomRight());

		cluster.add(lasvegas);
		Assert.assertEquals("Cluster size after adding a second point", 2, cluster.size());
		assertEquals("Center after adding a second point", new GeoPoint(37.915, -110.02), cluster.center());
		assertEquals("Top left corner after adding a second point", new GeoPoint(denver.getLat(), lasvegas.getLon()), cluster.bounds().topLeft());
		assertEquals("Bottom right corner after adding a second point", new GeoPoint(lasvegas.getLat(), denver.getLon()), cluster.bounds().bottomRight());

		cluster.add(sandiego);
		Assert.assertEquals("Cluster size after adding a third point", 3, cluster.size());
		assertEquals("Center after adding a third point", new GeoPoint(36.217, -112.39), cluster.center());
		assertEquals("Top left corner after adding a third point", new GeoPoint(denver.getLat(), sandiego.getLon()), cluster.bounds().topLeft());
		assertEquals("Bottom right corner after adding a third point", new GeoPoint(sandiego.getLat(), denver.getLon()), cluster.bounds().bottomRight());
	}

	private static void assertEquals(String message, GeoPoint expected, GeoPoint actual) {
		Assert.assertEquals(message + ", latitude", expected.getLat(), actual.getLat(), 0.001);
		Assert.assertEquals(message + ", longitude", expected.getLon(), actual.getLon(), 0.001);
	}
}
