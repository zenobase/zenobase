package com.zenobase.tasks.hexoskin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
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

/** Covers both Hexoskin task managers, which share {@link HexoskinTaskManagerSupport}. */
public class HexoskinTaskManagerTest extends TaskManagerTestSupport {

	private static final String TIMEZONE = "America/Los_Angeles";

	private final HexoskinCredentialsManager credentialsManager = mock(HexoskinCredentialsManager.class);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	@Test
	public void testActivities() {
		var manager = new HexoskinActivitiesTaskManager(credentialsManager);
		Task task = newTask(manager, "2013-07-01");
		assertThat(task.getMarker()).isEqualTo("2013-07-01");
		givenProfileThen(successResponse("HexoskinActivitiesResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(2);
		assertThat(requests.get(0).getUrl()).isEqualTo("https://api.hexoskin.com/api/v1/profile/");
		assertThat(requests.get(1).getUrl()).startsWith("https://api.hexoskin.com/api/v1/range/?");
		assertThat(queryParam(requests.get(1), "include_metrics")).isEqualTo("44,71,2003,2038");
		assertThat(queryParam(requests.get(1), "start__gt"))
			.as("midnight in the task's zone")
			.isEqualTo(ticks("2013-07-01T00:00:00-07:00"));

		assertThat(events(commands)).hasSize(7);
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).as("end of the latest activity").isEqualTo("2013-07-31T16:44:37.000-07:00");
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testMarkerWithOffsetIsUnchanged() {
		var manager = new HexoskinActivitiesTaskManager(credentialsManager);
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "Test").put("timezone", TIMEZONE).put("marker", "2013-07-01T00:00:00+09:00")
		);
		assertThat(task.getMarker()).isEqualTo("2013-07-01");
		task.setMarker("2013-07-01T05:30:00.000+09:00");
		givenProfileThen(response(200, "{\"meta\":{},\"objects\":[]}"));

		manager.execute(task, credentials);

		assertThat(queryParam(sentRequests(credentialsManager).get(1), "start__gt")).isEqualTo(
			ticks("2013-07-01T05:30:00+09:00")
		);
	}

	@Test
	public void testSleepFollowsNextLinks() {
		var manager = new HexoskinSleepTaskManager(credentialsManager);
		Task task = newTask(manager, "2014-03-01T00:00:00Z");
		givenProfileThen(
			response(200, "{\"meta\":{\"next\":\"/api/v1/range/?offset=100\"},\"objects\":[]}"),
			successResponse("HexoskinSleepResultTest.json")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(3);
		assertThat(queryParam(requests.get(1), "trainingroutine")).isEqualTo("12");
		assertThat(queryParam(requests.get(2), "offset")).isEqualTo("100");
		assertThat(events(commands)).hasSize(1);
		assertThat(task.getMarker()).as("end of the sleep").isEqualTo("2014-03-27T00:00:00.000-07:00");
	}

	@Test
	public void testEmpty() {
		var manager = new HexoskinSleepTaskManager(credentialsManager);
		Task task = newTask(manager, "2014-03-01T00:00:00Z");
		givenProfileThen(response(200, "{\"meta\":{},\"objects\":[]}"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(events(commands)).isEmpty();
		assertThat(task.getMarker()).isEqualTo("2014-03-01");
	}

	private static String ticks(String timestamp) {
		return String.valueOf((DateTime.parse(timestamp).getMillis() * 256) / 1000);
	}

	private static Task newTask(HexoskinTaskManagerSupport<?> manager, String marker) {
		return manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "Test").put("timezone", TIMEZONE).put("marker", marker)
		);
	}

	/** Every run starts by fetching the profile. */
	private void givenProfileThen(Response first, Response... rest) {
		Response[] more = new Response[rest.length + 1];
		more[0] = first;
		System.arraycopy(rest, 0, more, 1, rest.length);
		givenResponses(credentialsManager, credentials, successResponse("HexoskinProfileResultTest.json"), more);
	}
}
