package com.zenobase.json;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Location;
import com.zenobase.search.BoundingBoxConstraintBuilder;
import com.zenobase.search.DistanceConstraintBuilder;
import com.zenobase.search.ExistsConstraintBuilder;

public class LocationField extends Field<Location> {

	private final NestedField<BigDecimal> latitude = nest(new DecimalField("lat"));
	private final NestedField<BigDecimal> longitude = nest(new DecimalField("lon"));

	public LocationField(String name) {
		super(name, Location.class, "geo_point");
		addConstraintBuilder(name, new ExistsConstraintBuilder(getPath()));
		addConstraintBuilder(name, new BoundingBoxConstraintBuilder(getPath()));
		addConstraintBuilder(name, new DistanceConstraintBuilder(getPath()));
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("lat_lon", true);
	}

	@Override
	protected Location getValue(JsonNode node) {
		BigDecimal lat = latitude.getValue((ObjectNode) node);
		BigDecimal lon = longitude.getValue((ObjectNode) node);
		return new Location(lat, lon);
	}

	@Override
	public JsonNode toJson(Location value) {
		return value != null
			? toJson(value.getLatitude(), value.getLongitude())
			: NullNode.getInstance();
	}

	private JsonNode toJson(BigDecimal lat, BigDecimal lon) {
		ObjectNode node = Nodes.newObject();
		latitude.setValue(node, lat);
		longitude.setValue(node, lon);
		return node;
	}
}
