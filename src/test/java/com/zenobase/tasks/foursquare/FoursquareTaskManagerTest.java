package com.zenobase.tasks.foursquare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;

public class FoursquareTaskManagerTest {

	@Test
	public void test() {

		FoursquareCredentialsManager credentialsManager = mock(FoursquareCredentialsManager.class);
		FoursquareTaskManager manager = new FoursquareTaskManager(credentialsManager);
		OAuthCredentials credentials = mock(OAuthCredentials.class);

		String bucketId = Generator.id();
		Identity principal = new Identity();
		ObjectNode settings = Nodes.newObject();

		Task task = manager.newTask(bucketId, principal, settings);

		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getCompleted()).isNull();
		assertThat(task.getStatus()).isNull();
		assertThat(task.isStale()).isTrue();
		assertThat(task.getMarker()).isNull();
		assertThat(task.getUndoId()).isNull();

		Response response = mock(Response.class);
		when(response.isSuccessful()).thenReturn(true);
		when(response.getBody()).thenReturn("{}");
		when(credentialsManager.send(any(OAuthRequest.class), eq(credentials))).thenReturn(response);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getCompleted()).isNotNull();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.isStale()).isFalse();
		assertThat(task.getMarker()).isNotNull();
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	private Task apply(Command command, Task task) {
		if (command instanceof UpdateTaskCommand) {
			return ((UpdateTaskCommand) command).apply(task);
		} else if (command instanceof CompoundCommand) {
			return apply(Iterables.getOnlyElement(((CompoundCommand) command).getCommands()), task);
		} else {
			throw new IllegalArgumentException();
		}
	}
}
