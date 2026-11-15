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
	public void testLaterMarkerAdvances() {
		task.setMarker("2020-01-01T00:00:00.000Z");
		DateTime latest = DateTime.parse("2020-01-02T00:00:00.000Z");
		assertThat(task.laterMarker(DateTime.parse(task.getMarker()), latest)).isEqualTo(latest.toString());
		assertThat(task.laterMarker(null, latest)).as("without a current marker").isEqualTo(latest.toString());
	}

	@Test
	public void testLaterMarkerNeverMovesBackwards() {
		task.setMarker("2020-01-01T00:00:00.000Z");
		DateTime current = DateTime.parse(task.getMarker());
		assertThat(task.laterMarker(current, current.minusDays(1))).isEqualTo(task.getMarker());
		assertThat(task.laterMarker(current, current)).isEqualTo(task.getMarker());
		assertThat(task.laterMarker(current, null)).isEqualTo(task.getMarker());
	}

	@Test
	public void testFailedTaskIsStale() {
		task.setStatus(Status.FAILED);
		task.setCompleted(DateTime.now());
		assertThat(task.isStale()).isTrue();
	}
}
