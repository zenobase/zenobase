package com.zenobase.search;

import java.util.List;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilder;
import play.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class SearchBuilderSupport {

	private final Set<Widget> widgets = Sets.newLinkedHashSet();
	private final List<QueryBuilder> constraints = Lists.newArrayList();

	public SearchBuilderSupport addWidgets(String[] widgets) {
		if (widgets != null) {
			for (String widget : widgets) {
				addWidget(widget);
			}
		}
		return this;
	}

	public SearchBuilderSupport addWidget(String widget) {
		WidgetOptions options = WidgetOptions.parse(widget);
		String type = options.get("type");
		WidgetBuilder builder = getWidgetBuilders().get(type);
		if (builder == null) {
			Logger.warn("Widget builder not registered: " + type);
			return this;
		}
		return addWidget(builder.build(options));
	}

	public SearchBuilderSupport addWidget(Widget widget) {
		widgets.add(widget);
		return this;
	}

	public SearchBuilderSupport addConstraints(String[] expressions) {
		if (expressions != null) {
			for (String expression : expressions) {
				addConstraint(expression);
			}
		}
		return this;
	}

	public SearchBuilderSupport addConstraint(String expression) {
		String[] tokens = expression.split(":", 2);
		String field = tokens[0];
		String value = tokens[1];
		for (Constraint constraint : getConstraintBuilders().get(field)) {
			QueryBuilder builder = constraint.build(field, value);
			if (builder != null) {
				return addConstraint(builder);
			}
		}
		throw new IllegalArgumentException("Don't know what to do with constraint: " + expression);
	}

	public SearchBuilderSupport addConstraint(QueryBuilder builder) {
		constraints.add(builder);
		return this;
	}

	protected abstract ImmutableMap<String, WidgetBuilder> getWidgetBuilders();

	protected abstract ImmutableMultimap<String, Constraint> getConstraintBuilders();

	public Search build() {
		return new Search(widgets, constraints);
	}
}
