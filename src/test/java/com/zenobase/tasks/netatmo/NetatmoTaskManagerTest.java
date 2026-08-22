package com.zenobase.tasks.netatmo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class NetatmoTaskManagerTest extends TaskManagerTestSupport {

	private final NetatmoCredentialsManager credentialsManager = mock(NetatmoCredentialsManager.class);
	private final NetatmoTaskManager manager = new NetatmoTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	public NetatmoTaskManagerTest() {
		when(credentials.getToken()).thenReturn(new Token("key", "secret"));
	}

	@Test
	public void test() {
		String bucketId = Generator.id();
		Identity principal = new Identity();
		Task task = newTask(bucketId, principal);
		givenResponses(
			credentialsManager,
			credentials,
			successResponse("StationsResultTest.json"),
			successResponse("MeasurementsResultTest-5min.json")
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		// a recent marker keeps it to one device and one measurement window
		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(2);
		assertThat(requests.get(0).getCompleteUrl()).contains("getstationsdata");
		assertThat(requests.get(1).getCompleteUrl()).contains("getmeasure");
		assertThat(queryParam(requests.get(1), "scale")).isEqualTo("max");

		assertThat(events(commands)).hasSize(10);
		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getCompleted()).isNotNull();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.isStale()).isFalse();
		assertThat(task.getMarker()).as("last measurement + 1s").isEqualTo("1364516161");
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testNoDevices() {
		Task task = newTask(Generator.id(), new Identity());
		String marker = task.getMarker();
		givenResponses(credentialsManager, credentials, response(200, Nodes.newObject("status", "ok").toString()));

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(events(commands)).isEmpty();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isEqualTo(marker);
	}

	// The 401 and 500 mappings live in OAuthTaskManager.send, shared by all task managers.

	@Test
	public void testInvalidToken() {
		Task task = newTask(Generator.id(), new Identity());
		givenResponses(credentialsManager, credentials, response(401, "{}"));

		assertThatThrownBy(() -> manager.execute(task, credentials)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	public void testInvalidStatus() {
		Task task = newTask(Generator.id(), new Identity());
		givenResponses(credentialsManager, credentials, response(500, "boom"));

		assertThatThrownBy(() -> manager.execute(task, credentials)).isInstanceOf(InvalidStatusException.class);
	}

	private Task newTask(String bucketId, Identity principal) {
		ObjectNode settings = Nodes.newObject().put("marker", DateTime.now(DateTimeZone.UTC).minusDays(2).toString());
		return manager.newTask(bucketId, principal, settings);
	}
}
