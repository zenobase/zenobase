package com.zenobase.tasks.wakatime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

public class WakaTimeTaskManagerTest extends TaskManagerTestSupport {

	private static final DateTime NOW = DateTime.parse("2020-06-10T12:00:00Z");

	private final WakaTimeCredentialsManager credentialsManager = mock(WakaTimeCredentialsManager.class);
	private final WakaTimeTaskManager manager = new WakaTimeTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);
	private final Response noData = durations();

	public WakaTimeTaskManagerTest() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
		givenNow(NOW);
	}

	@Test
	public void test() {
		DateTime marker = NOW.minusDays(1);
		Task task = newTask(marker);
		DateTime recent = marker.plusMinutes(1).withMillisOfSecond(0);
		givenResponses(
			credentialsManager,
			credentials,
			durations(marker.minusMinutes(1), recent),
			// WakaTime answers 400 for days without data
			response(400, "{\"data\":[],\"timezone\":\"UTC\"}"),
			noData
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests)
			.as("one request per day through tomorrow")
			.extracting(request -> queryParam(request, "date"))
			.containsExactly("2020-06-09", "2020-06-10", "2020-06-11");
		assertThat(requests.getFirst().getUrl()).isEqualTo("https://wakatime.com/api/v1/users/current/durations");

		List<Event> events = events(commands);
		assertThat(events).as("only the duration after the marker").hasSize(1);
		assertThat(events.getFirst().getValues(Event.TAG)).containsExactly("coding", "zenobase");
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isEqualTo(recent.toString());
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testFreeAccountSkipsToTwoWeeksAgo() {
		Task task = newTask(NOW.minusDays(30));
		givenResponses(credentialsManager, credentials, response(402, "{}"), noData);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(1 + 14);
		assertThat(queryParam(requests.get(0), "date")).isEqualTo("2020-05-11");
		assertThat(queryParam(requests.get(1), "date")).isEqualTo("2020-05-29");
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
	}

	@Test
	public void testExpiredTokenIsRefreshed() {
		expireToken(credentialsManager, credentials);
		Task task = newTask(NOW);
		givenResponses(credentialsManager, credentials, noData);

		Command commands = manager.execute(task, credentials);

		assertThat(commands(commands, UpdateCredentialsCommand.class)).hasSize(1);
	}

	private Task newTask(DateTime marker) {
		return manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "coding").put("marker", marker.toString())
		);
	}

	private Response durations(DateTime... times) {
		ObjectNode root = Nodes.newObject();
		root.put("timezone", "UTC");
		ArrayNode data = root.putArray("data");
		for (DateTime time : times) {
			data.addObject()
				.put("time", time.getMillis() / 1000.0)
				.put("duration", 60)
				.put("project", "zenobase");
		}
		return response(200, root.toString());
	}
}
