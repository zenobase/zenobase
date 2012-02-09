package render;

import java.io.PrintWriter;

import models.Rating;
import schema.Field;

public class RatingRenderer extends Renderer<Rating> {

	public RatingRenderer() {
		super(Rating.class);
	}

	@Override
	public void render(Field<Rating> field, Rating value, PrintWriter out) {
		out.printf("<span title=\"Rated %s/5\">", value.getValue() / 20);
		for (int i = 0; i < 5; ++i) {
			out.printf("<i class=\"%s\"></i>", value.getValue() / 20 > i ? "icon-star" : "icon-star-empty");
		}
		out.print("</span>");
	}
}
