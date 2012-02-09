package tags;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.util.Map;

import models.Event;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;
import render.DateTimeRenderer;
import render.HeightRenderer;
import render.LengthRenderer;
import render.LocationRenderer;
import render.RatingRenderer;
import render.Renderer;
import render.ResourceRenderer;
import render.TokenRenderer;
import schema.Field;

import com.google.common.collect.ImmutableMap;

@FastTags.Namespace("zeno")
public class FieldTag extends FastTags {

	private static final ImmutableMap<Field<?>, Renderer<?>> renderers = 
		new ImmutableMap.Builder<Field<?>, Renderer<?>>()
			.put(Event.TAG, new TokenRenderer())
			.put(Event.RESOURCE, new ResourceRenderer())
			.put(Event.LENGTH, new LengthRenderer())
			.put(Event.HEIGHT, new HeightRenderer())
			.put(Event.LOCATION, new LocationRenderer())
			.put(Event.DATE_TIME, new DateTimeRenderer())
			.put(Event.RATING, new RatingRenderer())
			.build();

	public static void _fields(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
		Event event = (Event) args.get("value");
		for (Field<?> field : renderers.keySet()) {
			render(field, event, out);
		}
	}

	private static <T> void render(Field<T> field, Event event, PrintWriter out) {
		for (T value : event.get(field)) {
			out.printf("<span style=\"white-space:%s\">", get(field).canWrap() ? "normal" : "nowrap");
			get(field).render(field, value, out);
			out.print("</span> &nbsp; ");
		}
	}

	private static <T> Renderer<T> get(Field<T> field) {
		return (Renderer<T>) renderers.get(field);
	}
}
