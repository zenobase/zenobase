package render;

import java.io.PrintWriter;

import org.joda.time.DateTime;

import schema.Field;

import com.ocpsoft.pretty.time.PrettyTime;

public class DateTimeRenderer extends Renderer<DateTime> {

	public DateTimeRenderer() {
		super(DateTime.class);
	}

	@Override
	public void render(Field<DateTime> field, DateTime value, PrintWriter out) {
		out.printf("<i class=\"icon-time\" title=\"%s\"></i> <span title=\"%s\">%s</span>", field.getName(), value.toString(), new PrettyTime().format(value.toDate()));
	}
}
