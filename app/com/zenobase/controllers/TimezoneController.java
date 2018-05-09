package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;
import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.mvc.Controller;
import play.mvc.Result;

import com.zenobase.json.Nodes;
import com.zenobase.models.Location;

public class TimezoneController extends Controller {

	public Promise<Result> get(String lat, String lon) {
		return lat != null ? find(new Location(lat, lon)) : find();
	}

	public Promise<Result> find(Location location) {
		return createRequest(location).get()
			.map(response -> {
				Preconditions.checkState(response.getStatus() == OK,
					"Expected 200 status but got %d", response.getStatus());
				return Nodes.readObject(response.getBody());
			})
			.map((F.Function<ObjectNode, Result>) node -> {
				Preconditions.checkState("OK".equals(node.path("status").textValue()),
					"Expected 'OK' status but got %s", node);
				return ok(node);
			})
			.recover(t -> {
				Logger.error("Couldn't look up timezone for " + location, t);
				return internalServerError();
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
		return Promise.pure(ok(Nodes.newArray(DateTimeZone.getAvailableIDs())));
    }
}
