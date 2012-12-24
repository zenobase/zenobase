package com.zenobase.search;

import com.zenobase.common.Generator;

public class WidgetTestSupport extends SearchTestSupport {

	protected static final String WIDGET_ID = Generator.id();

	protected void addWidget(String options, Object... args) {
		getSearch().addWidget(String.format(options, args));
	}
}
