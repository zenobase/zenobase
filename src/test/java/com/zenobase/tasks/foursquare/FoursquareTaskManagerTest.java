package com.zenobase.tasks.foursquare;

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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class FoursquareTaskManagerTest extends TaskManagerTestSupport {

	private static final int LIMIT = 100;

	private final FoursquareCredentialsManager credentialsManager = mock(FoursquareCredentialsManager.class);
	private final FoursquareTaskManager manager = new FoursquareTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	@Test
	public void test() {
		String bucketId = Generator.id();
		Identity principal = new Identity();
		Task task = manager.newTask(bucketId, principal, Nodes.newObject());
		givenResponses(credentialsManager, credentials, successResponse("FoursquareResultTest.json"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		OAuthRequest request = onlyRequest(credentialsManager);
		assertThat(request.getCompleteUrl()).contains("users/self/checkins");
		assertThat(queryParam(request, "offset")).isEqualTo("0");

		assertThat(events(commands)).hasSize(3);
		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getCompleted()).isNotNull();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.isStale()).isFalse();
		assertThat(task.getMarker()).isNotNull();
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testPagination() {
		Task task = newTask();
		givenResponses(credentialsManager, credentials, page(250, LIMIT), page(250, LIMIT), page(250, 50));

		Command commands = manager.execute(task, credentials);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests)
			.extracting(request -> queryParam(request, "offset"))
			.containsExactly("0", "100", "200");
		assertThat(events(commands)).hasSize(250);
	}

	@Test
	public void testEmpty() {
		Task task = newTask();
		givenResponses(credentialsManager, credentials, response(200, "{}"));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(events(commands)).isEmpty();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isNotNull();
	}

	private Task newTask() {
		return manager.newTask(Generator.id(), new Identity(), Nodes.newObject());
	}

	/** A check-ins response reporting {@code count} total check-ins and carrying {@code items} of them. */
	private Response page(int count, int items) {
		ObjectNode root = Nodes.newObject();
		root.putObject("meta").put("code", 200);
		ObjectNode checkins = root.putObject("response").putObject("checkins");
		checkins.put("count", count);
		ArrayNode array = checkins.putArray("items");
		for (int i = 0; i < items; i++) {
			ObjectNode item = array.addObject();
			item.put("createdAt", 1354253026L + i);
			item.put("timeZoneOffset", -480);
			item.putObject("venue").put("name", "Venue " + i);
		}
		return response(200, root.toString());
	}
}
