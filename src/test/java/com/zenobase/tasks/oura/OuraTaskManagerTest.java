package com.zenobase.tasks.oura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;

/** Covers the three Oura task managers, which share {@link OuraTaskManagerSupport}. */
public class OuraTaskManagerTest extends TaskManagerTestSupport {

	private static final String MARKER = "2018-12-01T00:00:00+02:00";
	private static final String TIMEZONE = "Europe/Berlin";

	private final OuraCredentialsManager credentialsManager = mock(OuraCredentialsManager.class);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	public OuraTaskManagerTest() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
	}

	@Test
	public void testSleep() {
		var manager = new OuraSleepTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), settings().put("tag", "Sleep"));
		givenResponses(credentialsManager, credentials, successResponse("OuraSleepResultTest.json"));

		String tomorrowBefore = tomorrow();
		Command commands = manager.execute(task, credentials);
		String tomorrowAfter = tomorrow();
		task = apply(commands, task);

		OAuthRequest request = onlyRequest(credentialsManager);
		assertThat(request.getCompleteUrl()).contains("api.ouraring.com/v2/usercollection/sleep");
		assertThat(queryParam(request, "start_date")).isEqualTo("2018-12-01");
		assertThat(queryParam(request, "end_date")).isIn(tomorrowBefore, tomorrowAfter);

		assertThat(events(commands)).hasSize(3);
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getCompleted()).isNotNull();
		assertThat(task.getMarker()).as("latest bedtime").isEqualTo("2018-12-21T07:29:00.000+02:00");
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testSteps() {
		var manager = new OuraStepsTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), settings().put("timezone", TIMEZONE));
		givenResponses(credentialsManager, credentials, successResponse("OuraStepsResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(onlyRequest(credentialsManager).getCompleteUrl()).contains("/v2/usercollection/daily_activity");
		assertThat(events(commands)).hasSize(3);
		assertThat(task.getMarker()).as("end of the last day").isEqualTo("2018-12-05T00:00:00.000+01:00");
	}

	@Test
	public void testReadiness() {
		var manager = new OuraReadinessTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), settings().put("timezone", TIMEZONE));
		givenResponses(credentialsManager, credentials, successResponse("OuraReadinessResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(onlyRequest(credentialsManager).getCompleteUrl()).contains("/v2/usercollection/daily_readiness");
		assertThat(events(commands)).hasSize(3);
		assertThat(task.getMarker()).as("end of the last day").isEqualTo("2018-12-04T00:00:00.000+01:00");
	}

	@Test
	public void testOlderRecordsKeepMarker() {
		var manager = new OuraSleepTaskManager(credentialsManager);
		String marker = "2018-12-21T12:00:00.000+02:00";
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject().put("marker", marker));
		givenResponses(credentialsManager, credentials, successResponse("OuraSleepResultTest.json"));

		task = apply(manager.execute(task, credentials), task);

		assertThat(queryParam(onlyRequest(credentialsManager), "start_date")).isEqualTo("2018-12-21");
		assertThat(task.getMarker()).as("all bedtimes end before the marker").isEqualTo(marker);
	}

	@Test
	public void testEmpty() {
		var manager = new OuraStepsTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), settings());
		String marker = task.getMarker();
		givenResponses(credentialsManager, credentials, response(200, "{\"data\":[]}"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(events(commands)).isEmpty();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isEqualTo(marker);
	}

	@Test
	public void testExpiredTokenIsRefreshed() {
		expireToken(credentialsManager, credentials);
		var manager = new OuraStepsTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), settings());
		givenResponses(credentialsManager, credentials, successResponse("OuraStepsResultTest.json"));

		Command commands = manager.execute(task, credentials);

		assertThat(commands(commands, UpdateCredentialsCommand.class)).hasSize(1);
	}

	private ObjectNode settings() {
		return Nodes.newObject().put("marker", MARKER);
	}

	private static String tomorrow() {
		return DateTime.now(DateTime.parse(MARKER).getZone()).plusDays(1).toLocalDate().toString();
	}
}
