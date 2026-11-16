package com.zenobase.tasks.rescuetime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class RescueTimeProductivityTaskManagerTest extends TaskManagerTestSupport {

	private final RescueTimeCredentialsManager credentialsManager = mock(RescueTimeCredentialsManager.class);
	private final RescueTimeProductivityTaskManager manager = new RescueTimeProductivityTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	/** Two days before "now", so a single weekly request reaches the present. */
	private final LocalDateTime marker = LocalDateTime.parse("2020-06-08T12:00:00");

	public RescueTimeProductivityTaskManagerTest() {
		givenNow(DateTime.parse("2020-06-10T12:00:00Z"));
	}

	@Test
	public void test() {
		Task task = manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "Productivity").put("marker", marker.toString())
		);
		givenResponses(credentialsManager, credentials, rows(-1, 1, 2, 3));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		OAuthRequest request = onlyRequest(credentialsManager);
		assertThat(request.getUrl()).isEqualTo("https://www.rescuetime.com/api/oauth/data");
		assertThat(queryParam(request, "restrict_kind")).isEqualTo("efficiency");
		assertThat(queryParam(request, "restrict_begin")).isEqualTo(marker.toLocalDate().toString());

		assertThat(events(commands)).as("drops the hour before the marker and the hour still in progress").hasSize(2);
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isEqualTo(hour(2).toString());
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testWalksForwardOneWeekAtATime() {
		LocalDateTime older = marker.minusWeeks(1);
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject().put("marker", older.toString()));
		String before = task.getMarker();
		givenResponses(credentialsManager, credentials, rows());

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(sentRequests(credentialsManager))
			.extracting(request -> queryParam(request, "restrict_begin"))
			.containsExactly("2020-06-01", "2020-06-08");
		assertThat(events(commands)).isEmpty();
		assertThat(task.getMarker()).isEqualTo(before);
	}

	@Test
	public void testNoMarkerImportsAllHistory() {
		Task task = manager.newTask(Generator.id(), new Identity(), Nodes.newObject());
		givenResponses(credentialsManager, credentials, rows(1, 2, 3));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		OAuthRequest request = onlyRequest(credentialsManager);
		assertThat(request.getCompleteUrl()).doesNotContain("restrict_begin");
		assertThat(events(commands)).hasSize(2);
		assertThat(task.getMarker()).isEqualTo(hour(2).toString());
	}

	private DateTime hour(int offset) {
		return marker.plusHours(offset).toDateTime(DateTimeZone.UTC);
	}

	/** One row per hour, offset from the marker. */
	private Response rows(int... offsets) {
		ObjectNode root = Nodes.newObject();
		ArrayNode rows = root.putArray("rows");
		for (int offset : offsets) {
			rows.addArray().add(marker.plusHours(offset).toString()).add(600).add(1).add(1.5).add(87.5);
		}
		return response(200, root.toString());
	}
}
