package com.zenobase.tasks.foursquare;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import com.google.common.collect.Iterables;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthTask;
import com.zenobase.tasks.Task.Status;

public class FoursquareTaskManagerTest {

	private final OAuthService oauth = mock(OAuthService.class);

	@Test
	public void test() {

		FoursquareTaskManager manager = spy(new FoursquareTaskManager("", "", ""));
		doReturn(oauth).when(manager).getService(any(OAuthTask.class));

		String bucketId = Generator.id();
		Identity principal = new Identity();
		ObjectNode settings = Nodes.newObject();

		Token requestToken = Token.empty();
		Token accessToken = new Token("fee", "fie");
		String authorizationUrl = "localhost";

		when(oauth.getAuthorizationUrl(requestToken)).thenReturn(authorizationUrl);

		OAuthTask task = manager.newTask(bucketId, principal, settings);

		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getToken()).isEqualTo(requestToken);
		assertThat(task.getAuthorizationUrl()).isEqualTo(authorizationUrl);
		assertThat(task.getCompleted()).isNull();
		assertThat(task.getStatus()).isNull();
		assertThat(task.isStale()).isFalse();
		assertThat(task.getMarker()).isNull();
		assertThat(task.getUndoId()).isNull();

		when(oauth.getAccessToken(eq(requestToken), any(Verifier.class))).thenReturn(accessToken);

		Command command = manager.authorize(task, Nodes.newObject("code", "baz"));
		task = apply(command, task);

		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getToken()).isEqualTo(accessToken);
		assertThat(task.getAuthorizationUrl()).isNull();
		assertThat(task.getCompleted()).isNull();
		assertThat(task.getStatus()).isNull();
		assertThat(task.isStale()).isTrue();
		assertThat(task.getMarker()).isNull();
		assertThat(task.getUndoId()).isNull();

		Response response = mock(Response.class);
		doReturn(response).when(manager).send(any(OAuthRequest.class));
		when(response.isSuccessful()).thenReturn(true);
		when(response.getBody()).thenReturn("{}");

		Command commands = manager.execute(task);
		task = apply(commands, task);

		assertThat(task.getBucketId()).isEqualTo(bucketId);
		assertThat(task.getPrincipal()).isEqualTo(principal);
		assertThat(task.getToken()).isEqualTo(accessToken);
		assertThat(task.getAuthorizationUrl()).isNull();
		assertThat(task.getCompleted()).isNotNull();
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.isStale()).isFalse();
		assertThat(task.getMarker()).isNotNull();
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	private OAuthTask apply(Command command, OAuthTask task) {
		if (command instanceof UpdateTaskCommand) {
			return ((UpdateTaskCommand) command).apply(task).as(OAuthTask.class);
		} else if (command instanceof CompoundCommand) {
			return apply(Iterables.getOnlyElement(((CompoundCommand) command).getCommands()), task);
		} else {
			throw new IllegalArgumentException();
		}
	}
}
