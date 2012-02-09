package render;

import java.io.PrintWriter;

import models.Length;
import models.Rating;
import schema.Field;

public class HeightRenderer extends Renderer<Length> {

	public HeightRenderer() {
		super(Length.class);
	}

	@Override
	public void render(Field<Length> field, Length value, PrintWriter out) {
		out.print("<span title=\"Height\" style=\"white-space:nowrap\">");
		out.printf("<i class=\"icon-resize-vertical\"></i>%s%s", value.getValue(), value.getUnit());
		out.print("</span>");
	}
}
