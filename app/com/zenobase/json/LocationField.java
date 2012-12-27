package com.zenobase.json;

import java.math.BigDecimal;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.models.Location;
import com.zenobase.search.BoundingBoxConstraintBuilder;
import com.zenobase.search.DistanceConstraintBuilder;

public class LocationField extends Field<Location> {

	private static final DecimalField LATITUDE = new DecimalField("lat");
	private static final DecimalField LONGITUDE = new DecimalField("lon");

	public LocationField(String name) {
		super(name, Location.class, "geo_point");
		addConstraint(new BoundingBoxConstraintBuilder());
		addConstraint(new DistanceConstraintBuilder());
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
	public JsonNode toJson(Location value) {
		return value != null
			? toJson(value.getLatitude(), value.getLongitude())
			: NullNode.getInstance();
	}

	private static JsonNode toJson(BigDecimal lat, BigDecimal lon) {
		ObjectNode node = Nodes.newObject();
		LATITUDE.setValue(node, lat);
		LONGITUDE.setValue(node, lon);
		return node;
	}
}
