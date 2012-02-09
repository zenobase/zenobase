package render;

import java.io.PrintWriter;

import models.Length;
import models.Rating;
import schema.Field;

public class LengthRenderer extends Renderer<Length> {

	public LengthRenderer() {
		super(Length.class);
	}

	@Override
	public void render(Field<Length> field, Length value, PrintWriter out) {
		out.print("<span title=\"Length\" style=\"white-space:nowrap\">");
		out.printf("<i class=\"icon-resize-horizontal\"></i> %s%s", value.getValue(), value.getUnit());
		out.print("</span>");
	}
}
