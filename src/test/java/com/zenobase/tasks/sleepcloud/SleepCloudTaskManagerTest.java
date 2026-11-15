package com.zenobase.tasks.sleepcloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManagerTestSupport;
import com.zenobase.tasks.google.GoogleCredentialsManager;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.scribe.model.Token;

public class SleepCloudTaskManagerTest extends TaskManagerTestSupport {

	private final GoogleCredentialsManager credentialsManager = mock(GoogleCredentialsManager.class);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);
	private final SleepCloudTaskManager manager = new SleepCloudTaskManager(credentialsManager);

	public SleepCloudTaskManagerTest() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("SleepCloudResultTest.json"),
			response(200, "{\"sleeps\":[]}")
		);
	}

	@Test
	public void testMarkerAdvancesToLatestWakeUp() {
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject().put("tag", "sleep"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(events(commands)).hasSize(3);
		assertThat(DateTime.parse(task.getMarker())).isEqualTo(DateTime.parse("2014-03-10T06:26:50-05:00"));
	}

	@Test
	public void testOlderRecordsKeepMarker() {
		// fetchRecords returns records modified since the marker, which can include older nights
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject().put("tag", "sleep"));
		String marker = "2014-03-11T00:00:00.000-05:00";
		task.setMarker(marker);

		task = apply(manager.execute(task, credentials), task);

		assertThat(task.getMarker()).isEqualTo(marker);
	}
}
