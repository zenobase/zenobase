package com.zenobase.tasks.moves;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.zenobase.common.LocationMap;
import com.zenobase.models.Location;

class MovesStorylineResult {

	private final JsonNode node;

	public MovesStorylineResult(JsonNode node) {
		this.node = node;
	}

	public void update(LocationMap locations) {
		for (JsonNode dayNode : node) {
			addDay(dayNode, locations);
		}
	}

	public void addDay(JsonNode node, LocationMap locations) {
		for (JsonNode segmentNode : node.path("segments")) {
			addSegment(segmentNode, locations);
		}
	}

	public void addSegment(JsonNode node, LocationMap locations) {
		String type = node.path("type").textValue();
		if ("move".equals(type)) {
			TrackPoint previous = null;
			for (TrackPoint trackPoint : getTrackPoints(node.path("activities"))) {
				if (previous != null) {
					locations.put(previous.getTime(), trackPoint.getTime(), previous.getLocation());
				}
				previous = trackPoint;
			}

		} else if ("place".equals(type)) {
			DateTime begin = dateTimeValue(node.path("startTime"));
			DateTime end = dateTimeValue(node.path("endTime"));
			Location location = locationValue(node.path("place").path("location"));
			locations.put(begin, end, location);
		}
	}

	public List<TrackPoint> getTrackPoints(JsonNode node) {
		List<TrackPoint> trackPoints = Lists.newArrayList();
		for (JsonNode activityNode : node) {
			for (JsonNode trackPointNode : activityNode.path("trackPoints")) {
				DateTime time = dateTimeValue(trackPointNode.path("time"));
				Location location = locationValue(trackPointNode);
				trackPoints.add(new TrackPoint(time, location));
			}
		}
		return trackPoints;
	}

	private static DateTime dateTimeValue(JsonNode node) {
		return !node.isMissingNode()
			? DateTime.parse(node.textValue(), ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed())
			: null;
	}

	private static Location locationValue(JsonNode node) {
		JsonNode lat = node.path("lat");
		JsonNode lon = node.path("lon");
		return !lat.isMissingNode() && !lon.isMissingNode()
			? new Location(lat.decimalValue(), lon.decimalValue())
			: null;
	}

	private static class TrackPoint {

		private final DateTime time;
		private final Location location;

		public TrackPoint(DateTime time, Location location) {
			this.time = time;
			this.location = location;
		}

		public DateTime getTime() {
			return time;
		}

		public Location getLocation() {
			return location;
		}
	}
}
