package com.zenobase.services;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;

public abstract class NodeFactorySupport implements NodeFactory {

	@Override
	public abstract Node createNode(String clusterName);

	protected ImmutableSettings.Builder createDefaultSettings() {
		return ImmutableSettings.settingsBuilder()
			.put("index.mapper.dynamic", false)
			.put("index.cache.filter.type", "none")
			.put("index.cache.field.type", "soft")
			.put("action.auto_create_index", false);
	}
}
