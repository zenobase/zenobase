package com.zenobase.tasks.bodymedia;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class BodyMediaSummaryTaskManager extends BodyMediaTaskManagerSupport {

	@Inject
	public BodyMediaSummaryTaskManager(BodyMediaCredentialsManager credentialsManager) {
		super(BodyMediaSummaryTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		return new BodyMediaSummaryTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(BodyMediaSummaryTask.class), credentials);
	}

	private Command execute(BodyMediaSummaryTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		OAuthRequest request = createRequest(task);
		checkRateLimit();
		Response response = send(request, credentials);
		BodyMediaSummaryResult result = new BodyMediaSummaryResult(parseObject(response), task.getPrincipal());
		return createCommand(task, credentials, result.getLastSyncDate().toLocalDate(), result.getEvents(), token);
	}

	private OAuthRequest createRequest(BodyMediaSummaryTask task) {
		return new OAuthRequest(Verb.GET, String.format("https://api.bodymedia.com/v2/json/summary/day/%s/%s", formatMarker(getFromDate(task)), formatMarker(new LocalDate())));
	}

	private static LocalDate getFromDate(Task task) {
		return LocalDate.parse(task.getMarker());
	}
}
