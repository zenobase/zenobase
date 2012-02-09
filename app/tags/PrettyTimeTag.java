package tags;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.util.Map;

import org.joda.time.DateTime;

import com.ocpsoft.pretty.time.PrettyTime;

import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;

@FastTags.Namespace("zeno")
public class PrettyTimeTag extends FastTags {

	public static void _prettyTime(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
		DateTime dateTime = (DateTime) args.get("value");
		out.print(new PrettyTime().format(dateTime.toDate()));
	}
}
