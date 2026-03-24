package com.zenobase.tasks;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class TaskManagerRegistry {

	private final Map<String, TaskManager> managers = Maps.newHashMap();

	@Inject
	public TaskManagerRegistry(Set<TaskManager> managers) {
		for (TaskManager manager : managers) {
			this.managers.put(manager.getType(), manager);
		}
	}

	public TaskManager find(String type) {
		Preconditions.checkNotNull(type);
		TaskManager manager = managers.get(type);
		Preconditions.checkNotNull(manager, "Missing manager for task type '%s': " + managers.keySet(), type);
		return manager;
	}
}
