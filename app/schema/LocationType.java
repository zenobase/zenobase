package schema;

import java.math.BigDecimal;

import models.Location;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import common.Nodes;

public class LocationType extends Type<Location> {

	private static final String LATITUDE = "lat";
	private static final String LONGITUDE = "lon";

	public LocationType() {
		super(Location.class, "geo_point");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("lat_lon", true);
	}

	@Override
	protected Location get(JsonNode node) {
		BigDecimal latitude = node.get(LATITUDE).getDecimalValue();
		BigDecimal longitude = node.get(LONGITUDE).getDecimalValue();
		return new Location(latitude, longitude);
	}

	@Override
	protected JsonNode toJson(Location value) {
		ObjectNode object = Nodes.newObject();
		object.put(LATITUDE, value.getLatitude());
		object.put(LONGITUDE, value.getLongitude());
		return object;
	}

}
