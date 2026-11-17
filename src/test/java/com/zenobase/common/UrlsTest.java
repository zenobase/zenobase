package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UrlsTest {

	@Test
	public void testRedactsCredentials() {
		assertThat(
			Urls.redact("https://api.netatmo.com/api/getmeasure?device_id=70%3Aee&access_token=abc%7C123&scale=max")
		).isEqualTo("https://api.netatmo.com/api/getmeasure?device_id=70%3Aee&access_token=REDACTED&scale=max");
		assertThat(
			Urls.redact("https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&sk=s&api_key=k&api_sig=x")
		).isEqualTo(
			"https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&sk=REDACTED&api_key=REDACTED&api_sig=REDACTED"
		);
		assertThat(Urls.redact("https://example.com/token?CLIENT_SECRET=s&code=c&refresh_token=$1#x")).isEqualTo(
			"https://example.com/token?CLIENT_SECRET=REDACTED&code=REDACTED&refresh_token=REDACTED#x"
		);
	}

	@Test
	public void testKeepsOtherUrls() {
		assertThat(Urls.redact("https://wbsapi.withings.net/v2/sleep?action=get&startdate=1")).isEqualTo(
			"https://wbsapi.withings.net/v2/sleep?action=get&startdate=1"
		);
		assertThat(Urls.redact("https://example.com/token")).isEqualTo("https://example.com/token");
	}
}
