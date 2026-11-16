package com.zenobase.tasks.lastfm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidCredentialsException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import java.util.List;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;

public class LastFmTaskManagerTest extends TaskManagerTestSupport {

	private final LastFmCredentialsManager credentialsManager = mock(LastFmCredentialsManager.class);
	private final LastFmTaskManager manager = new LastFmTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);
	private final Task task = manager.newTask(
		Generator.id(),
		new Identity(),
		Nodes.newObject().put("marker", "2014-02-21T00:00:00").put("timezone", "America/Los_Angeles")
	);

	public LastFmTaskManagerTest() {
		when(credentials.getScope()).thenReturn("tester");
	}

	@Test
	public void test() {
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("RecentTracksResultTest.json"),
			response(200, "{\"recenttracks\":{\"track\":[],\"@attr\":{\"page\":\"2\",\"totalPages\":\"2\"}}}"),
			successResponse("TrackInfoResultTest.json")
		);

		Command commands = manager.execute(task, credentials);
		Task updated = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests)
			.as("two pages, then one lookup per MusicBrainz recording")
			.hasSize(2 + 6);
		assertThat(queryParam(requests.get(0), "method")).isEqualTo("user.getrecenttracks");
		assertThat(queryParam(requests.get(0), "user")).isEqualTo("tester");
		assertThat(queryParam(requests.get(0), "from")).isEqualTo("1392969600");
		assertThat(queryParam(requests.get(0), "format")).isEqualTo("json");
		assertThat(queryParam(requests.get(1), "page")).isEqualTo("2");
		assertThat(queryParam(requests.get(2), "method")).isEqualTo("track.getinfo");
		assertThat(queryParam(requests.get(2), "mbid")).isEqualTo("9dbadd08-ae0e-4d33-b3d6-43a9eb42bee0");

		List<Event> events = events(commands);
		assertThat(events).hasSize(9);
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(163));
		assertThat(events.get(1).getValue(Event.DURATION)).as("no MusicBrainz id").isNull();
		assertThat(events)
			.filteredOn(event -> event.getValue(Event.DURATION) != null)
			.hasSize(6);
		assertThat(updated.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(updated.getMarker()).isEqualTo(queryParam(requests.get(0), "to"));
	}

	@Test
	public void testForbiddenIsInvalidCredentials() {
		givenResponses(credentialsManager, credentials, response(403, "{}"));

		assertThatThrownBy(() -> manager.execute(task, credentials)).isInstanceOf(InvalidCredentialsException.class);
	}
}
