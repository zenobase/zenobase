package com.zenobase.tasks.openmhealth;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.dropbox.DropboxCredentialsManager;
import com.zenobase.tasks.dropbox.ListFolderResult;

public class HipboneTaskManager extends OAuthTaskManager {

	@Inject
	public HipboneTaskManager(DropboxCredentialsManager credentialsManager) {
		super(HipboneTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String folder = Preconditions.checkNotNull(settings.path("folder").textValue());
		folder = folder.replace('\\', '/');
		if (!folder.startsWith("/")) {
			folder = "/" + folder;
		}
		return new HipboneTask(bucketId, principal, folder);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(HipboneTask.class), credentials);
	}

	private Command execute(HipboneTask task, OAuthCredentials credentials) {
		ListFolderResult folder = list(credentials, task.getFolder(), task.getMarker());
		List<Event> events = Lists.newArrayList();
		for (String file : folder.getFiles()) {
			Event event = getEvent(credentials, task.getPrincipal(), task.getFolder() + "/" + file);
			if (event != null) {
				events.add(event);
			}
		}
		return createCommand(task, folder.getCursor(), events);
	}

	private ListFolderResult list(OAuthCredentials credentials, String folder, String cursor) {
		OAuthRequest request = null;
		if (cursor != null) {
			request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/2/files/list_folder/continue");
			request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
			request.addPayload(Nodes.newObject("cursor", cursor).toString());
		} else {
			request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/2/files/list_folder");
			request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
			request.addPayload(Nodes.newObject("path", folder).toString());
		}
		Response response = send(request, credentials);
		return new ListFolderResult(parse(response));
	}

	private Event getEvent(OAuthCredentials credentials, Identity author, String path) {
		String url = String.format("https://content.dropboxapi.com/2/files/download");
		OAuthRequest request = new OAuthRequest(Verb.POST, url);
		request.addHeader(HttpHeaders.CONTENT_TYPE, "");
		request.addHeader("Dropbox-API-Arg", Nodes.newObject("path", path).toString());
		Response response = send(request, credentials);
		return new DataPointResult(author, parse(response)).getEvent();
	}

	private Command createCommand(Task task, String cursor, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran hipbone task", "reverted hipbone task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), cursor)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
