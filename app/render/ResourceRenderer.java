package render;

import java.io.PrintWriter;

import models.Location;
import models.Resource;
import schema.Field;

public class ResourceRenderer extends Renderer<Resource> {

	public ResourceRenderer() {
		super(Resource.class);
		setWrap(true);
	}

	@Override
	public void render(Field<Resource> field, Resource value, PrintWriter out) {
		out.printf("<i class=\"icon-bookmark\" title=\"%s\"></i>&nbsp;<a href=\"%s\">%s</a>",
			field.getName(), value.getUrl(), value.getTitle());
	}
}
