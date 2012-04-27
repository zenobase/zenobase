package com.zenobase.json;

import java.math.BigDecimal;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.common.Nodes;
import com.zenobase.models.Location;

public class LocationField extends Field<Location> {

	private static final DecimalField LATITUDE = new DecimalField("lat");
	private static final DecimalField LONGITUDE = new DecimalField("lon");

	public LocationField(String name) {
		super(name, Location.class, "geo_point");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("lat_lon", true);
	}

	@Override
	protected Location getValue(JsonNode node) {
		BigDecimal latitude = LATITUDE.getValue((ObjectNode) node);
		BigDecimal longitude = LONGITUDE.getValue((ObjectNode) node);
		return new Location(latitude, longitude);
	}

	@Override
	protected JsonNode toJson(Location value) {
		ObjectNode node = Nodes.newObject();
		LATITUDE.setValue(node, value.getLatitude());
		LONGITUDE.setValue(node, value.getLongitude());
		return node;
	}
}
