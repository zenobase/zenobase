package com.zenobase.tasks.reporter;

import java.util.List;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.dropbox.DropboxCredentialsManager;
import com.zenobase.tasks.dropbox.DropboxTaskManagerSupport;
import com.zenobase.tasks.dropbox.ListFolderResult;

public class ReporterTaskManager extends DropboxTaskManagerSupport {

	@Inject
	public ReporterTaskManager(DropboxCredentialsManager credentialsManager) {
		super(ReporterTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String folder = Preconditions.checkNotNull(settings.path("folder").textValue());
		folder = folder.replace('\\', '/');
		return new ReporterTask(bucketId, principal, folder);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(ReporterTask.class), credentials);
	}

	private Command execute(ReporterTask task, OAuthCredentials credentials) {
		Configuration config = getConfiguration(credentials, task.getFolder());
		SortedSet<LocalDate> dates = getDates(credentials, task.getFolder(), task.getFirstDate());
		List<Event> events = Lists.newArrayList();
		for (LocalDate date : dates) {
			getEvents(credentials, config, task.getPrincipal(), task.getFolder(), date, events);
		}
		return createCommand(task, Iterables.getLast(dates, task.getFirstDate()), events);
	}

	private Configuration getConfiguration(OAuthCredentials credentials, String folder) {
		return new ConfigurationResult(download(credentials, "/" + folder + "/zenobase-conf.json")).get();
	}


	private SortedSet<LocalDate> getDates(OAuthCredentials credentials, String folder, LocalDate firstDate) {
		SortedSet<LocalDate> dates = Sets.newTreeSet();
		ListFolderResult result;
		String cursor = null;
		do {
			result = list(credentials, "/" + folder, cursor);
			for (String file : result.getFiles()) {
				LocalDate date = parseLocalDate(file);
				if (date != null && (firstDate == null || !date.isBefore(firstDate))) {
					dates.add(date);
				}
			}
			cursor = result.getCursor();
		} while (result.hasMore());
		return dates;
	}

	private static LocalDate parseLocalDate(String filename) {
		Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})-reporter-export\\.json");
		Matcher m = p.matcher(filename);
		return m.find() ? LocalDate.parse(m.group(1)) : null;
	}

	private void getEvents(OAuthCredentials credentials, Configuration config, Identity author, String folder, LocalDate date, List<Event> events) {
		String path = String.format("/%s/%s-reporter-export.json", folder, date);
		events.addAll(new SnapshotsResult(config, author, download(credentials, path)).getEvents());
	}

	private Command createCommand(Task task, LocalDate marker, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran reporter task", "reverted reporter task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker != null ? marker.toString() : null)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
