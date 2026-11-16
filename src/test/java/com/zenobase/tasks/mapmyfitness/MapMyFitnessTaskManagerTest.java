package com.zenobase.tasks.mapmyfitness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

/** Covers the three MapMyFitness task managers, which share {@link MapMyFitnessTaskManagerSupport}. */
public class MapMyFitnessTaskManagerTest extends TaskManagerTestSupport {

	private static final String MARKER = "2015-01-01T00:00:00.000-08:00";

	private final MapMyFitnessCredentialsManager credentialsManager = mock(MapMyFitnessCredentialsManager.class);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	public MapMyFitnessTaskManagerTest() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
	}

	@Test
	public void testActivities() {
		var manager = new MapMyFitnessActivitiesTaskManager(credentialsManager);
		Task task = newTask(manager, "2014-04-01T00:00:00.000-07:00");
		Response type = successResponse("TypeResultTest.json");
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("UserResultTest.json"),
			successResponse("WorkoutsResultTest.json"),
			type,
			type,
			successResponse("RouteResultTest.json")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests)
			.extracting(OAuthRequest::getUrl)
			.containsExactly(
				"https://oauth2-api.mapmyapi.com/v7.0/user/self/",
				"https://oauth2-api.mapmyapi.com/v7.0/workout/",
				"https://oauth2-api.mapmyapi.com/v7.0/activity_type/9/",
				"https://oauth2-api.mapmyapi.com/v7.0/activity_type/285/",
				"https://oauth2-api.mapmyapi.com/v7.0/route/400640020/"
			);
		assertThat(queryParam(requests.get(1), "user")).isEqualTo("12345678");
		assertThat(queryParam(requests.get(1), "started_after")).isEqualTo("2014-04-01T00:00:00.000-07:00");

		List<Event> events = events(commands);
		assertThat(events).hasSize(2);
		assertThat(events.get(0).getValues(Event.TAG)).containsExactly("Hike / Rock Climb");
		assertThat(events.get(0).getValue(Event.LOCATION)).isEqualTo(new Location("47.62688383", "-122.36104538"));
		assertThat(events.get(1).getValue(Event.LOCATION)).isNull();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).as("a second after the latest workout").isEqualTo("2014-11-06T11:00:01.000-08:00");
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testWeight() {
		var manager = new MapMyFitnessWeightTaskManager(credentialsManager);
		Task task = newTask(manager, MARKER);
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("UserResultTest.json"),
			successResponse("WeightResultTest.json")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		OAuthRequest request = sentRequests(credentialsManager).get(1);
		assertThat(request.getUrl()).endsWith("/api/0.1/bodymass/");
		assertThat(queryParam(request, "target_start_datetime")).isEqualTo(MARKER);
		assertThat(events(commands))
			.hasSize(2)
			.allMatch(event -> event.getValues(Event.TAG).contains("body"));
		assertThat(task.getMarker()).isEqualTo("2015-01-07T08:11:08.000-06:00");
	}

	@Test
	public void testSleep() {
		var manager = new MapMyFitnessSleepTaskManager(credentialsManager);
		Task task = newTask(manager, MARKER);
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("UserResultTest.json"),
			successResponse("SleepResultTest.json")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(sentRequests(credentialsManager).get(1).getUrl()).endsWith("/api/0.2/sleep/");
		assertThat(events(commands)).hasSize(1);
		assertThat(task.getMarker()).as("a second after waking up").isEqualTo("2015-01-07T05:30:01.000-08:00");
	}

	@Test
	public void testFollowsNextLinks() {
		var manager = new MapMyFitnessSleepTaskManager(credentialsManager);
		Task task = newTask(manager, MARKER);
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("UserResultTest.json"),
			response(200, "{\"_links\":{\"next\":[{\"href\":\"/api/0.2/sleep/?offset=100\"}]}}"),
			response(200, "{}")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(sentRequests(credentialsManager))
			.extracting(OAuthRequest::getUrl)
			.containsExactly(
				"https://oauth2-api.mapmyapi.com/v7.0/user/self/",
				"https://oauth2-api.mapmyapi.com/api/0.2/sleep/",
				"https://oauth2-api.mapmyapi.com/api/0.2/sleep/?offset=100"
			);
		assertThat(events(commands)).isEmpty();
		assertThat(task.getMarker()).isEqualTo(MARKER);
	}

	@Test
	public void testExpiredTokenIsRefreshed() {
		expireToken(credentialsManager, credentials);
		var manager = new MapMyFitnessWeightTaskManager(credentialsManager);
		givenResponses(credentialsManager, credentials, successResponse("UserResultTest.json"));

		Command commands = manager.execute(newTask(manager, MARKER), credentials);

		assertThat(commands(commands, UpdateCredentialsCommand.class)).hasSize(1);
	}

	private static Task newTask(MapMyFitnessTaskManagerSupport manager, String marker) {
		return manager.newTask(Generator.id(), new Identity(), Nodes.newObject().put("marker", marker));
	}
}
