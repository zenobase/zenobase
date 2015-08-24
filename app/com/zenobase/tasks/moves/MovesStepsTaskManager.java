package com.zenobase.tasks.moves;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MovesStepsTaskManager extends MovesTaskManagerSupport {

	@Inject
	public MovesStepsTaskManager(MovesCredentialsManager credentialsManager) {
		super(MovesStepsTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		Unit<Length> lengthUnit = Objects.firstNonNull(new UnitField<Length>("unit").getValue(settings), Units.M);
		return new MovesStepsTask(bucketId, principal, tag, lengthUnit);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MovesStepsTask.class), credentials);
	}

	private Command execute(MovesStepsTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime from = task.getFrom();
		if (from == null) {
			MovesProfileResult profile = getProfile(credentials);
			from = profile.getFirstDate();
		} else {
			from = from.plusDays(1);
		}
		List<Event> events = getEvents(task, credentials, from);
		removeLast(events);
		return createCommand(task, credentials, events, token);
	}

	private List<Event> getEvents(MovesStepsTask task, OAuthCredentials credentials, DateTime begin) {
		List<Event> events = Lists.newArrayList();
		LocalDate today = LocalDate.now(begin.getZone());
		for (LocalDate from = begin.toLocalDate(); !from.isAfter(today); from = from.withDayOfMonth(1).plusMonths(1)) {
			LocalDate to = min(from.dayOfMonth().withMaximumValue(), today);
			SummaryQuery request = new SummaryQuery(task.getPrincipal(), begin.getZone(), task.getTag(), task.getUnit(), credentials);
			events.addAll(request.find(from, to).getEvents());
		}
		return events;
	}

	private class SummaryQuery {

		private final Identity principal;
		private final DateTimeZone zone;
		private final String tag;
		private final Unit<Length> lengthUnit;
		private final OAuthCredentials credentials;

		public SummaryQuery(Identity principal, DateTimeZone zone, String tag, Unit<Length> lengthUnit, OAuthCredentials credentials) {
			this.principal = principal;
			this.zone = zone;
			this.tag = tag;
			this.lengthUnit = lengthUnit;
			this.credentials = credentials;
		}

		public MovesSummaryResult find(LocalDate from, LocalDate to) {
			OAuthRequest request = newRequest("/user/summary/daily");
			request.addQuerystringParameter("from", from.toString());
			request.addQuerystringParameter("to", to.toString());
			Response response = send(request, credentials);
			return new MovesSummaryResult(parseArray(response), principal, zone, tag, lengthUnit);
		}
	}
}
