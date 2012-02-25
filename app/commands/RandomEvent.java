package commands;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Random;

import models.Bucket;
import models.Event;
import models.Length;
import models.Location;
import models.Rating;
import models.Resource;
import models.Text;
import models.Token;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import common.Generator;
import common.RandomElement;

class RandomEvent {

	private static final RandomElement<Builder> builders = new RandomElement<Builder>()
		.add(new Builder() {
			RandomElement<Token> meals = new RandomElement<Token>()
				.add(Token.valueOf("lunch"), 1)
				.add(Token.valueOf("dinner"), 1);
			RandomElement<Token> order = new RandomElement<Token>()
				.add(Token.valueOf("pizza"), 1)
				.add(Token.valueOf("sushi"), 1)
				.add(Token.valueOf("mexican"), 2)
				.add(Token.valueOf("sandwich"), 2)
				.add(Token.valueOf("chinese"), 2);
			@Override
			protected void addFields(Event event) {
				event.add(Event.TAG, meals.next());
				event.add(Event.TAG, order.next());
				event.add(Event.LOCATION, nextLocation());
				event.add(Event.RATING, nextRating());
			}
		}, 4)
		.add(new Builder() {
			@Override
			protected void addFields(Event event) {
				event.add(Event.TAG, Token.valueOf("sleep"));
			}
		}, 2)
		.add(new Builder() {
			RandomElement<Movie> movies = new MovieParser().parse(new File("data/movies.tsv"));
			@Override
			protected void addFields(Event event) {
				Movie movie = movies.next();
				event.add(Event.TAG, Token.valueOf("movie"));
				event.add(Event.RESOURCE, movie.getResource());
				if (movie.getDuration() != null) {
					event.add(Event.DURATION, movie.getDuration());
				}
				event.add(Event.RATING, nextRating());
			}
		}, 2)
		.add(new Builder() {
			@Override
			protected void addFields(Event event) {
				event.add(Event.TAG, Token.valueOf("hike"));
				event.add(Event.DURATION, nextDuration(30, 330));
				event.add(Event.LOCATION, nextLocation());
				event.add(Event.DISTANCE, nextLength(500, 10000));
				event.add(Event.HEIGHT, nextLength(0, 5000));
			}
		}, 1);

	private final Bucket bucket;

	public RandomEvent(Bucket bucket) {
		this.bucket = bucket;
	}

	public Event next() {
		return builders.next().build(bucket);
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

		public Event build(Bucket bucket) {
			Event event = new Event(Generator.id(), bucket.getId());
			event.set(Event.CREATOR, bucket.getIdentity());
			event.add(Event.TIMESTAMP, nextTimestamp());
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
			BigDecimal lat = BigDecimal.valueOf(rand.nextInt(18000) - 9000).movePointLeft(2);
			BigDecimal lon = BigDecimal.valueOf(rand.nextInt(36000) - 18000).movePointLeft(2);
			return new Location(lat, lon);
		}

		protected Rating nextRating() {
			return ratings.next();
		}

		protected Length nextLength(int min, int max) {
			return Length.valueOf(BigDecimal.valueOf(min + rand.nextInt(10) * (max / 10)), Length.Unit.m);
		}
	}

	private static class Movie {

		private final Resource resource;
		private final Duration duration;

		public Movie(Text title, Token url, Duration duration) {
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
						resources.add(new Movie(Text.valueOf(title), Token.valueOf(url), duration), weight);
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