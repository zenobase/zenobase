package com.zenobase.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

/**
 * Support for testing {@code *TaskManager} classes end-to-end without real credentials or network access.
 *
 * <p>The only point where a task manager touches the network is {@link OAuthCredentialsManager#send}. Mocking that
 * method lets us feed recorded JSON fixtures into the manager and assert on the {@link Command}s it emits.
 */
public abstract class TaskManagerTestSupport extends ResultTestSupport {

	/** Fixes Joda's clock, so that managers paging up to "now" make a predictable number of requests. */
	protected static void givenNow(DateTime now) {
		DateTimeUtils.setCurrentMillisFixed(now.getMillis());
	}

	@AfterEach
	public void resetClock() {
		DateTimeUtils.setCurrentMillisSystem();
	}

	/** An HTTP 200 response whose body is the named fixture, resolved next to the test class. */
	protected Response successResponse(String fixture) {
		try {
			return response(200, new String(readBytes(fixture), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	protected Response response(int code, String body) {
		return RecordingTransport.response(code, body);
	}

	/** Scripts the responses to successive {@code send()} calls; the last one repeats. */
	protected void givenResponses(
		OAuthCredentialsManager credentialsManager,
		OAuthCredentials credentials,
		Response first,
		Response... rest
	) {
		when(credentialsManager.send(any(OAuthRequest.class), eq(credentials))).thenReturn(first, rest);
	}

	/** Marks the credentials expired; reauthorizing them swaps in a new token, as a real refresh would. */
	protected void expireToken(OAuthCredentialsManager credentialsManager, OAuthCredentials credentials) {
		var token = new AtomicReference<>(new Token("old", "secret"));
		when(credentials.isExpired()).thenReturn(true);
		when(credentials.getToken()).thenAnswer(invocation -> token.get());
		when(credentials.getPrincipal()).thenReturn(new Identity());
		when(credentials.getId()).thenReturn(Generator.id());
		doAnswer(invocation -> {
			token.set(new Token("new", "secret"));
			return null;
		})
			.when(credentialsManager)
			.reauthorize(credentials);
	}

	protected List<OAuthRequest> sentRequests(OAuthCredentialsManager credentialsManager) {
		ArgumentCaptor<OAuthRequest> captor = ArgumentCaptor.forClass(OAuthRequest.class);
		verify(credentialsManager, atLeastOnce()).send(captor.capture(), any(OAuthCredentials.class));
		return captor.getAllValues();
	}

	protected OAuthRequest onlyRequest(OAuthCredentialsManager credentialsManager) {
		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(1);
		return requests.getFirst();
	}

	protected static String queryParam(OAuthRequest request, String name) {
		String value = RecordingTransport.queryParams(request).get(name);
		if (value == null) {
			throw new AssertionError("Missing query parameter '" + name + "' in " + request.getCompleteUrl());
		}
		return value;
	}

	/**
	 * Applies the task-state changes in a command to produce the resulting task. Fails on a credentials update, which
	 * only a refreshed token should produce (see {@link #expireToken}).
	 */
	protected Task apply(Command command, Task task) {
		assertThat(commands(command, UpdateCredentialsCommand.class)).as("credentials updates").isEmpty();
		for (UpdateTaskCommand update : commands(command, UpdateTaskCommand.class)) {
			task = update.apply(task);
		}
		return task;
	}

	protected List<Event> events(Command command) {
		List<Event> events = new ArrayList<>();
		for (CreateEventsCommand create : commands(command, CreateEventsCommand.class)) {
			events.addAll(create.getEvents());
		}
		return events;
	}

	/** All sub-commands of the given type, flattened across nested compound commands. */
	protected <T extends Command> List<T> commands(Command command, Class<T> type) {
		List<T> found = new ArrayList<>();
		for (Command c : flatten(command)) {
			if (type.isInstance(c)) {
				found.add(type.cast(c));
			}
		}
		return found;
	}

	private static List<Command> flatten(Command command) {
		List<Command> flat = new ArrayList<>();
		if (command instanceof CompoundCommand compound) {
			for (Command c : compound.getCommands()) {
				flat.addAll(flatten(c));
			}
		} else {
			flat.add(command);
		}
		return flat;
	}
}
