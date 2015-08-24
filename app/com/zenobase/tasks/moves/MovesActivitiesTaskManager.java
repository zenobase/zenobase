package com.zenobase.tasks.moves;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
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

public class MovesActivitiesTaskManager extends MovesTaskManagerSupport {

	@Inject
	public MovesActivitiesTaskManager(MovesCredentialsManager credentialsManager) {
		super(MovesActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		Unit<Length> lengthUnit = Objects.firstNonNull(new UnitField<Length>("unit").getValue(settings), Units.M);
		return new MovesActivitiesTask(bucketId, principal, lengthUnit, Units.KCAL);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MovesActivitiesTask.class), credentials);
	}

	private Command execute(MovesActivitiesTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime from = task.getFrom();
		if (from == null) {
			MovesProfileResult profile = getProfile(credentials);
			from = profile.getFirstDate();
		}
		List<Event> events = getEvents(task, credentials, from);
		removeDuplicates(events);
		removeLast(events);
		return createCommand(task, credentials, events, token);
	}

	private List<Event> getEvents(MovesActivitiesTask task, OAuthCredentials credentials, DateTime begin) {
		List<Event> events = Lists.newArrayList();
		LocalDate today = LocalDate.now(begin.getZone());
		for (LocalDate from = begin.toLocalDate(); !from.isAfter(today); from = from.withDayOfMonth(1).plusMonths(1)) {
			LocalDate to = min(from.dayOfMonth().withMaximumValue(), today);
			ActivitiesQuery request = new ActivitiesQuery(task.getPrincipal(), begin, task.getUnit(), task.getEnergyUnit(), credentials);
			events.addAll(request.find(from, to).getEvents());
		}
		return events;
	}

	private class ActivitiesQuery {

		private final Identity principal;
		private final DateTime begin;
		private final Unit<Length> lengthUnit;
		private final Unit<Energy> energyUnit;
		private final OAuthCredentials credentials;

		public ActivitiesQuery(Identity principal, DateTime begin, Unit<Length> lengthUnit, Unit<Energy> energyUnit, OAuthCredentials credentials) {
			this.principal = principal;
			this.begin = begin;
			this.lengthUnit = lengthUnit;
			this.energyUnit = energyUnit;
			this.credentials = credentials;
		}

		public MovesActivitiesResult find(LocalDate from, LocalDate to) {
			OAuthRequest request = newRequest("/user/activities/daily");
			request.addQuerystringParameter("from", from.toString());
			request.addQuerystringParameter("to", to.toString());
			Response response = send(request, credentials);
			return new MovesActivitiesResult(parseArray(response), principal, begin, lengthUnit, energyUnit);
		}
	}
}
