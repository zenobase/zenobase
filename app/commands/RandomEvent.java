package commands;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Random;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import models.Event;
import models.Location;
import models.Rating;
import models.Resource;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import secure.Identity;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import common.Generator;
import common.RandomElement;

class RandomEvent {

	private static final RandomElement<Builder> builders = new RandomElement<Builder>()
		.add(new Builder() {
			RandomElement<String> meals = new RandomElement<String>()
				.add("lunch", 1)
				.add("dinner", 1);
			RandomElement<String> order = new RandomElement<String>()
				.add("pizza", 1)
				.add("sushi", 1)
				.add("mexican", 2)
				.add("sandwich", 2)
				.add("greek", 2)
				.add("chinese", 2);
			@Override
			protected void addFields(Event event) {
				event.addValue(Event.TAG, meals.next());
				event.addValue(Event.TAG, order.next());
				event.setValue(Event.LOCATION, nextLocation());
				event.setValue(Event.RATING, nextRating());
			}
		}, 4)
		.add(new Builder() {
			@Override
			protected void addFields(Event event) {
				event.addValue(Event.TAG, "sleep");
			}
		}, 2)
		.add(new Builder() {
			RandomElement<Movie> movies = new MovieParser().parse(new File("data/movies.tsv"));
			@Override
			protected void addFields(Event event) {
				Movie movie = movies.next();
				event.addValue(Event.TAG, "movie");
				event.setValue(Event.RESOURCE, movie.getResource());
				if (movie.getDuration() != null) {
					event.setValue(Event.DURATION, movie.getDuration());
				}
				event.setValue(Event.RATING, nextRating());
			}
		}, 2)
		.add(new Builder() {
			@Override
			protected void addFields(Event event) {
				event.addValue(Event.TAG, "hike");
				event.setValue(Event.DURATION, nextDuration(30, 330));
				event.setValue(Event.LOCATION, nextLocation());
				event.setValue(Event.DISTANCE, nextLength(500, 10000));
				event.setValue(Event.HEIGHT, nextLength(0, 5000));
			}
		}, 1);

	private final String bucketId;
	private final Identity identity;

	public RandomEvent(String bucketId, Identity identity) {
		this.bucketId = bucketId;
		this.identity = identity;
	}

	public Event next() {
		return builders.next().build(bucketId, identity);
	}

	private static class Builder {

		private final Random rand = new Random();

		private final RandomElement<Rating> ratings = new RandomElement<Rating>()
			.add(Rating.valueOf(100), 2)
			.add(Rating.valueOf( 80), 6)
			.add(Rating.valueOf( 60), 4)
			.add(Rating.valueOf( 40), 2)
			.add(Rating.valueOf( 20), 2)
			.add(Rating.valueOf(  0), 1);

		public Event build(String bucketId, Identity identity) {
			Event event = new Event(Generator.id());
			event.setValue(Event.AUTHOR, identity);
			event.addValue(Event.TIMESTAMP, nextTimestamp());
			addFields(event);
			return event;
		}

		protected void addFields(Event event) {
			
		}

		protected DateTime nextTimestamp() {
			return new DateTime().minusMinutes(rand.nextInt(60 * 24 * 365)); // 1 year
		}

		protected Duration nextDuration(int minMinutes, int maxMinutes) {
			return Duration.standardMinutes(minMinutes + rand.nextInt(maxMinutes - minMinutes));
		}

		protected Location nextLocation() {
			BigDecimal lat = BigDecimal.valueOf(rand.nextInt(16000) - 8000).movePointLeft(2); // avoid the poles
			BigDecimal lon = BigDecimal.valueOf(rand.nextInt(36000) - 18000).movePointLeft(2);
			return new Location(lat, lon);
		}

		protected Rating nextRating() {
			return ratings.next();
		}

		protected DecimalMeasure<Length> nextLength(int min, int max) {
			return DecimalMeasure.valueOf(BigDecimal.valueOf(min + rand.nextInt(10) * (max / 10)), SI.METER);
		}
	}

	private static class Movie {

		private final Resource resource;
		private final Duration duration;

		public Movie(String title, String url, Duration duration) {
			this.resource = new Resource(title, url);
			this.duration = duration;
		}

		public Resource getResource() {
			return resource;
		}

		public Duration getDuration() {
			return duration;
		}
	}

	private static class MovieParser {

		public RandomElement<Movie> parse(File source) {
			try {
				return Files.readLines(source, Charsets.UTF_8, new LineProcessor<RandomElement<Movie>>() {
					private final RandomElement<Movie> resources = new RandomElement<Movie>();
					@Override
					public boolean processLine(String line) {
						String[] tokens = line.split("\t");
						String title = String.format("%s (%d)", tokens[5], Integer.parseInt(tokens[11]));
						String url = tokens[15];
						Duration duration = !tokens[10].isEmpty() ? Duration.standardMinutes(Integer.parseInt(tokens[10])) : null;
						int weight = Integer.parseInt(tokens[13]);
						resources.add(new Movie(title, url, duration), weight);
						return true;
					}
					@Override
					public RandomElement<Movie> getResult() {
						return resources;
					}
				});
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
	}
}