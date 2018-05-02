package com.zenobase.tasks.foursquare;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.zenobase.testing.ManualTests;

@Category(ManualTests.class)
public class FoursquareVenuesTesting {

	private final String apiKey = System.getProperty("oauth.apiKey");
	private final String apiSecret = System.getProperty("oauth.apiSecret");

	@Before
	public void setUp() {
		Assume.assumeNotNull(apiKey);
		Assume.assumeNotNull(apiSecret);
	}

	@Test
	public void test() {
		FoursquareVenues venues = new FoursquareVenues(apiKey, apiSecret);
		FoursquareVenue venue = venues.find("416dc180f964a5209b1d1fe3");
		assertThat(venue).isNotNull();
		assertThat(venue.toResource().getTitle()).isEqualTo("Space Needle");
		assertThat(venue.getCategories()).containsExactly("Landmark", "Historic Site", "Scenic Lookout");
	}

	@Test
	public void testNotFound() {
		FoursquareVenues venues = new FoursquareVenues(apiKey, apiSecret);
		FoursquareVenue venue = venues.find("xxxxxxxxxxxxxxxxxxxxxxxx");
		assertThat(venue).isSameAs(FoursquareVenue.UNKNOWN);
	}
}
