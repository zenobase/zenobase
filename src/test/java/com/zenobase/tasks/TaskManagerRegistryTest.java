package com.zenobase.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TaskManagerRegistryTest {

	@Test
	public void test() {
		TaskManager manager = mockTaskManager("foo");
		Set<TaskManager> managers = Sets.newHashSet();
		managers.add(manager);
		managers.add(mockTaskManager("bar"));
		TaskManagerRegistry registry = new TaskManagerRegistry(managers);
		assertThat(registry.find("foo")).isSameAs(manager);
	}

	private static TaskManager mockTaskManager(String type) {
		TaskManager manager = Mockito.mock(TaskManager.class);
		Mockito.when(manager.getType()).thenReturn(type);
		return manager;
	}
}
