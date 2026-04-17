package com.zenobase.json;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Location;
import com.zenobase.search.constraints.BoundingBoxConstraintBuilder;
import com.zenobase.search.constraints.DistanceConstraintBuilder;
import com.zenobase.search.constraints.ExistsConstraintBuilder;

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
	}

	@Override
	protected Location getValue(JsonNode node) {
		ObjectNode obj = (ObjectNode) node;
		JsonNode latNode = obj.get("lat");
		JsonNode lonNode = obj.get("lon");
		if (latNode == null || latNode.isNull() || lonNode == null || lonNode.isNull()) {
			throw new IllegalArgumentException("Null coordinate in location: " + node);
		}
		BigDecimal lat = Objects.requireNonNull(latitude.getValue(obj));
		BigDecimal lon = Objects.requireNonNull(longitude.getValue(obj));
		return new Location(lat, lon);
	}

	@Override
	public JsonNode toJson(@Nullable Location value) {
		return value != null ? toJson(value.latitude(), value.longitude()) : NullNode.getInstance();
	}

	private JsonNode toJson(BigDecimal lat, BigDecimal lon) {
		ObjectNode node = Nodes.newObject();
		latitude.setValue(node, lat);
		longitude.setValue(node, lon);
		return node;
	}
}
