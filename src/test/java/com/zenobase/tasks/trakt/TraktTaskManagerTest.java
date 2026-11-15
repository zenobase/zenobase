package com.zenobase.tasks.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

public class TraktTaskManagerTest extends TaskManagerTestSupport {

	private static final int LIMIT = 10;

	private final TraktCredentialsManager credentialsManager = mock(TraktCredentialsManager.class);
	private final TraktTaskManager manager = new TraktTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);
	private final Task task = manager.newTask(
		Generator.id(),
		new Identity(),
		Nodes.newObject().put("marker", "2015-04-04T04:30:00.000Z")
	);

	public TraktTaskManagerTest() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
	}

	@Test
	public void test() {
		givenFixtures();

		Command commands = manager.execute(task, credentials);
		Task updated = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(3);
		assertThat(requests.get(0).getCompleteUrl()).contains("api.trakt.tv/users/settings");
		assertThat(requests.get(1).getCompleteUrl()).contains("/users/me/history/movies");
		assertThat(requests.get(2).getCompleteUrl()).contains("/users/me/history/episodes");
		assertThat(queryParam(requests.get(1), "limit")).isEqualTo("10");
		assertThat(queryParam(requests.get(1), "page")).isEqualTo("1");
		assertThat(queryParam(requests.get(1), "extended")).isEqualTo("full");

		// one of two movies and both episodes are newer than the marker
		assertThat(events(commands)).hasSize(3);
		assertThat(updated.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(updated.getCompleted()).isNotNull();
		assertThat(updated.getMarker()).as("latest watch").isEqualTo("2015-04-14T23:30:00.000-07:00");
		assertThat(updated.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testPagesUntilAPartialPage() {
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("TraktSettingsResultTest.json"),
			movies(LIMIT),
			movies(3),
			response(200, "[]")
		);

		Command commands = manager.execute(task, credentials);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(4);
		assertThat(queryParam(requests.get(1), "page")).isEqualTo("1");
		assertThat(queryParam(requests.get(2), "page")).isEqualTo("2");
		assertThat(requests.get(3).getCompleteUrl()).contains("/history/episodes");
		assertThat(events(commands)).hasSize(LIMIT + 3);
	}

	@Test
	public void testEmpty() {
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("TraktSettingsResultTest.json"),
			response(200, "[]")
		);

		Command commands = manager.execute(task, credentials);
		Task updated = apply(commands, task);

		assertThat(events(commands)).isEmpty();
		assertThat(updated.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(updated.getMarker()).isEqualTo(task.getMarker());
	}

	@Test
	public void testExpiredTokenIsRefreshed() {
		expireToken(credentialsManager, credentials);
		givenFixtures();

		Command commands = manager.execute(task, credentials);

		assertThat(commands(commands, UpdateCredentialsCommand.class)).hasSize(1);
	}

	private void givenFixtures() {
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("TraktSettingsResultTest.json"),
			successResponse("TraktMoviesResultTest.json"),
			successResponse("TraktEpisodesResultTest.json")
		);
	}

	/** A page of watched movies, all recent enough to clear the marker. */
	private Response movies(int count) {
		ArrayNode array = Nodes.newArray();
		DateTime watchedAt = DateTime.parse("2015-06-01T00:00:00.000Z");
		for (int i = 0; i < count; i++) {
			ObjectNode item = array.addObject();
			item.put("watched_at", watchedAt.plusMinutes(i).toString());
			ObjectNode movie = item.putObject("movie");
			movie.put("title", "Movie " + i);
			movie.put("year", 2015);
			movie.put("runtime", 90);
			movie.putObject("ids").put("trakt", 1000 + i);
		}
		return response(200, array.toString());
	}
}
