package com.zenobase.controllers;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTimeZone;
import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import com.zenobase.json.Nodes;
import com.zenobase.models.Location;

public class TimezoneController extends Controller {

	public Promise<Result> get(String lat, String lon) {
		return lat != null ? find(new Location(lat, lon)) : find();
	}

	public Promise<Result> find(final Location location) {
		return createRequest(location).get()
			.map(new F.Function<WSResponse, ObjectNode>() {
				@Override
				public ObjectNode apply(WSResponse response) {
					Preconditions.checkState(response.getStatus() == OK,
						"Expected 200 status but got %d", response.getStatus());
					return Nodes.readObject(response.getBody());
				}
			})
			.map(new F.Function<ObjectNode, Result>() {
				@Override
				public Result apply(ObjectNode node) {
					Preconditions.checkState("OK".equals(node.path("status").textValue()),
						"Expected 'OK' status but got %s", node);
					return ok(node);
				}
			})
			.recover(new F.Function<Throwable, Result>() {
				@Override
				public Result apply(Throwable t) {
					Logger.error("Couldn't look up timezone for " + location, t);
					return internalServerError();
				}
			}
		);
	}

	private WSRequestHolder createRequest(Location location) {
		return WS.url("https://maps.googleapis.com/maps/api/timezone/json")
			.setQueryParameter("location", location.toString())
			.setQueryParameter("timestamp", Long.toString(System.currentTimeMillis() / 1000))
			.setQueryParameter("sensor", "false");
	}

	private Promise<Result> find() {
		List<String> timezones = Lists.newArrayList();
		for (String id : DateTimeZone.getAvailableIDs()) {
			timezones.add(id);
		}
		return Promise.<Result>pure(ok(Nodes.newArray(timezones)));
    }
}
