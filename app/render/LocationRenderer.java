package render;

import java.io.PrintWriter;

import models.Location;
import schema.Field;

public class LocationRenderer extends Renderer<Location> {

	public LocationRenderer() {
		super(Location.class);
	}

	@Override
	public void render(Field<Location> field, Location value, PrintWriter out) {
		out.printf("<i class=\"icon-map-marker\" title=\"%s\"></i> <a href=\"http://maps.google.com/maps?q=%s,%s&t=p&z=5\">%s, %s</a>",
			field.getName(), value.getLatitude(), value.getLongitude(), value.getLatitude(), value.getLongitude());
	}
}
