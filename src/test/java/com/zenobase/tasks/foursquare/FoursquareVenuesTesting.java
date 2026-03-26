package com.zenobase.tasks.foursquare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.testing.Manual;

@Manual
public class FoursquareVenuesTesting {

	private final String apiKey = System.getProperty("oauth.apiKey");
	private final String apiSecret = System.getProperty("oauth.apiSecret");

	@BeforeEach
	public void setUp() {
		Assumptions.assumeTrue(apiKey != null);
		Assumptions.assumeTrue(apiSecret != null);
	}

	@Test
	public void test() {
		FoursquareVenues venues = new FoursquareVenues(apiKey, apiSecret);
		FoursquareVenue venue = venues.find("416dc180f964a5209b1d1fe3");
		assertThat(venue).isNotNull();
		assertThat(venue.toResource().title()).isEqualTo("Space Needle");
		assertThat(venue.getCategories()).containsExactly("Landmark", "Historic Site", "Scenic Lookout");
	}

	@Test
	public void testNotFound() {
		FoursquareVenues venues = new FoursquareVenues(apiKey, apiSecret);
		FoursquareVenue venue = venues.find("xxxxxxxxxxxxxxxxxxxxxxxx");
		assertThat(venue).isSameAs(FoursquareVenue.UNKNOWN);
	}
}
