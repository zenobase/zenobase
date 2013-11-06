package com.zenobase.tasks;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class IntegrationManagerRegistry {

	private final Map<String, CredentialsManager> managers = Maps.newHashMap();

	@Inject
	public IntegrationManagerRegistry(Set<CredentialsManager> managers) {
		for (CredentialsManager manager : managers) {
			this.managers.put(manager.getType(), manager);
		}
	}

	public CredentialsManager find(String type) {
		Preconditions.checkNotNull(type);
		CredentialsManager manager = managers.get(type);
		Preconditions.checkNotNull(manager, "Missing manager for integration type '%s'", type);
		return manager;
	}
}
