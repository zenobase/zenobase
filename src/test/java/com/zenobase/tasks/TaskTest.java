package com.zenobase.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task.Status;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class TaskTest {

	private final Task task = new Task("test", Generator.id(), new Identity());

	@Test
	public void testNewTaskIsStale() {
		assertThat(task.isStale()).isTrue();
	}

	@Test
	public void testTaskCompletedJustNowIsNotStale() {
		task.setCompleted(DateTime.now());
		assertThat(task.isStale()).isFalse();
	}

	@Test
	public void testTaskCompletedMinutesAgoIsStale() {
		task.setCompleted(DateTime.now().minusMinutes(2));
		assertThat(task.isStale()).isTrue();
	}

	@Test
	public void testFailedTaskIsStale() {
		task.setStatus(Status.FAILED);
		task.setCompleted(DateTime.now());
		assertThat(task.isStale()).isTrue();
	}
}
