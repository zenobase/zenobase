package com.zenobase.tasks.withings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.InvalidTokenException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;

/**
 * Covers all five Withings task managers, which share {@link WithingsTaskManagerSupport}. Every send waits on that
 * class's rate limiter (2/s), so keep the number of requests down.
 */
public class WithingsTaskManagerTest extends TaskManagerTestSupport {

	private static final String TIMEZONE = "America/Los_Angeles";

	private final WithingsCredentialsManager credentialsManager = mock(WithingsCredentialsManager.class);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	public WithingsTaskManagerTest() {
		// an OAuth2 token, i.e. one without a secret; see testOAuth1TokenIsReauthorized
		when(credentials.getToken()).thenReturn(new Token("key", ""));
	}

	@Test
	public void testWeight() {
		var manager = new WithingsWeightTaskManager(credentialsManager);
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "body").put("unit", "lb").put("timezone", TIMEZONE)
		);
		givenResponses(credentialsManager, credentials, successResponse("WithingsWeightResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		OAuthRequest request = onlyRequest(credentialsManager);
		assertThat(request.getCompleteUrl()).contains("wbsapi.withings.net/measure");
		assertThat(queryParam(request, "action")).isEqualTo("getmeas");
		assertThat(queryParam(request, "category")).isEqualTo("1");

		assertThat(events(commands)).hasSize(2);
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isEqualTo("1353615011");
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testTemperature() {
		var manager = new WithingsTemperatureTaskManager(credentialsManager);
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "body").put("unit", "F").put("timezone", TIMEZONE)
		);
		givenResponses(credentialsManager, credentials, successResponse("WithingsTemperatureResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(queryParam(onlyRequest(credentialsManager), "meastype")).isEqualTo("71");
		assertThat(events(commands)).hasSize(3);
		assertThat(task.getMarker()).isEqualTo("1353615011");
	}

	@Test
	public void testCardio() {
		var manager = new WithingsCardioTaskManager(credentialsManager);
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "heart rate").put("timezone", TIMEZONE)
		);
		givenResponses(credentialsManager, credentials, successResponse("WithingsCardioResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(queryParam(onlyRequest(credentialsManager), "action")).isEqualTo("getmeas");
		assertThat(events(commands)).hasSize(3);
		assertThat(task.getMarker()).isEqualTo("1387899568");
	}

	@Test
	public void testSteps() {
		var manager = new WithingsStepsTaskManager(credentialsManager);
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "walk").put("unit", "mi").put("marker", "2013-12-01")
		);
		givenResponses(credentialsManager, credentials, successResponse("WithingsStepsResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		OAuthRequest request = onlyRequest(credentialsManager);
		assertThat(request.getCompleteUrl()).contains("wbsapi.withings.net/v2/measure");
		assertThat(queryParam(request, "action")).isEqualTo("getactivity");
		assertThat(queryParam(request, "startdateymd")).isEqualTo("2013-12-01");

		assertThat(events(commands)).hasSize(2);
		assertThat(task.getMarker()).isEqualTo("2013-12-16");
	}

	@Test
	public void testSleepWalksForwardOneWeekAtATime() {
		var manager = new WithingsSleepTaskManager(credentialsManager);
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "sleep").put("timezone", TIMEZONE).put("marker", daysAgo(8))
		);
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("WithingsSleepResultTest.json"),
			response(200, "{\"status\":0,\"body\":{\"series\":[]}}")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).as("one request per week between the marker and now").hasSize(2);
		assertThat(requests.getFirst().getCompleteUrl()).contains("wbsapi.withings.net/v2/sleep");
		assertThat(queryParam(requests.get(0), "enddate")).isEqualTo(queryParam(requests.get(1), "startdate"));

		assertThat(events(commands)).as("merged sleep periods").hasSize(2);
		assertThat(task.getMarker()).isEqualTo("2014-03-12T11:03:17.000-07:00");
	}

	@Test
	public void testUnauthorizedStatus() {
		var manager = new WithingsWeightTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject());
		// Withings signals authorization failures in the body, not the HTTP status
		givenResponses(credentialsManager, credentials, response(200, "{\"status\":401}"));

		assertThatThrownBy(() -> manager.execute(task, credentials)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	public void testInvalidStatus() {
		var manager = new WithingsCardioTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject());
		givenResponses(credentialsManager, credentials, response(200, "{\"status\":601}"));

		assertThatThrownBy(() -> manager.execute(task, credentials)).isInstanceOf(InvalidStatusException.class);
	}

	@Test
	public void testOAuth1TokenIsReauthorized() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
		var manager = new WithingsCardioTaskManager(credentialsManager);
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject().put("timezone", TIMEZONE));
		givenResponses(credentialsManager, credentials, successResponse("WithingsCardioResultTest.json"));

		manager.execute(task, credentials);

		verify(credentialsManager).reauthorize(credentials);
	}

	private static String daysAgo(int days) {
		return DateTime.now(DateTimeZone.UTC).minusDays(days).toString();
	}
}
