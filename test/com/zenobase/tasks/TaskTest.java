package com.zenobase.tasks;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task.Status;

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
	public void testUnauthorizedTaskIsNotStale() {
		task.setAuthorizationUrl("localhost");
		assertThat(task.isStale()).isFalse();
	}

	@Test
	public void testFailedTaskIsStale() {
		task.setStatus(Status.FAILED);
		task.setCompleted(DateTime.now());
		assertThat(task.isStale()).isTrue();
	}
}
