package com.zenobase.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;

public class CredentialsManagerRegistry {

	private final Map<String, CredentialsManager> managers = Maps.newHashMap();

	@Inject
	public CredentialsManagerRegistry(Set<CredentialsManager> managers) {
		for (CredentialsManager manager : managers) {
			this.managers.put(manager.getType(), manager);
		}
	}

	public boolean exists(String type) {
		return managers.containsKey(type);
	}

	public CredentialsManager find(String type) {
		Preconditions.checkNotNull(type);
		CredentialsManager manager = managers.get(type);
		Preconditions.checkNotNull(manager, "Missing manager for integration type '%s'", type);
		return manager;
	}
}
