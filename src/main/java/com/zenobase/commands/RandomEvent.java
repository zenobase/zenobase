package com.zenobase.commands;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.zenobase.common.RandomElement;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

public class RandomEvent {

	private static final RandomElement<Builder> builders = new RandomElement<Builder>()
		.add(
			new Builder() {
				final RandomElement<String> meals = new RandomElement<String>().add("lunch", 1).add("dinner", 1);
				final RandomElement<String> order = new RandomElement<String>()
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
			},
			4
		)
		.add(
			new Builder() {
				@Override
				protected void addFields(Event event) {
					event.addValue(Event.TAG, "sleep");
				}
			},
			2
		)
		.add(
			new Builder() {
				final RandomElement<Movie> movies = new MovieParser().parse(new File("data/movies.tsv"));

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
			},
			2
		)
		.add(
			new Builder() {
				@Override
				protected void addFields(Event event) {
					event.addValue(Event.TAG, "hike");
					event.setValue(Event.DURATION, nextDuration(30, 330));
					event.setValue(Event.LOCATION, nextLocation());
					event.setValue(Event.DISTANCE, nextLength(500, 10000));
					event.setValue(Event.HEIGHT, nextLength(0, 5000));
				}
			},
			1
		);

	private final Identity principal;

	public RandomEvent(Identity principal) {
		this.principal = principal;
	}

	public Event next() {
		return builders.next().build(principal);
	}

	private static class Builder {

		private final Random rand = new Random();

		private final RandomElement<Rating> ratings = new RandomElement<Rating>()
			.add(Rating.valueOf(100), 2)
			.add(Rating.valueOf(80), 6)
			.add(Rating.valueOf(60), 4)
			.add(Rating.valueOf(40), 2)
			.add(Rating.valueOf(20), 2)
			.add(Rating.valueOf(0), 1);

		public Event build(Identity principal) {
			Event event = new Event();
			event.setValue(Event.AUTHOR, principal);
			event.setValue(Event.TIMESTAMP, nextTimestamp());
			addFields(event);
			return event;
		}

		protected void addFields(Event event) {}

		protected DateTime nextTimestamp() {
			return DateTime.now().minusMinutes(rand.nextInt(60 * 24 * 365)); // 1 year
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
			return DecimalMeasure.valueOf(BigDecimal.valueOf(min + rand.nextInt(10) * (max / 10)), Units.M);
		}
	}

	private static class Movie {

		private final Resource resource;
		private final @Nullable Duration duration;

		public Movie(String title, String url, @Nullable Duration duration) {
			this.resource = new Resource(title, url);
			this.duration = duration;
		}

		public Resource getResource() {
			return resource;
		}

		public @Nullable Duration getDuration() {
			return duration;
		}
	}

	private static class MovieParser {

		public RandomElement<Movie> parse(File source) {
			try {
				return Objects.requireNonNull(
					Files.asCharSource(source, StandardCharsets.UTF_8).readLines(
						new LineProcessor<>() {
							private final RandomElement<Movie> resources = new RandomElement<>();

							@Override
							public boolean processLine(String line) {
								List<String> tokens = Splitter.on('\t').splitToList(line);
								String title = String.format(
									"%s (%d)",
									tokens.get(5),
									Integer.parseInt(tokens.get(11))
								);
								String url = tokens.get(15);
								Duration duration = !tokens.get(10).isEmpty()
									? Duration.standardMinutes(Integer.parseInt(tokens.get(10)))
									: null;
								int weight = Integer.parseInt(tokens.get(13));
								resources.add(new Movie(title, url, duration), weight);
								return true;
							}

							@Override
							public RandomElement<Movie> getResult() {
								return resources;
							}
						}
					)
				);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
	}
}
