package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class UrlsTest {

	@ParameterizedTest
	@CsvSource({
		"https://x.com/?device_id=70%3Aee&access_token=abc%7C123, https://x.com/?device_id=70%3Aee&access_token=REDACTED",
		"https://x.com/?method=m&sk=s&api_key=k&api_sig=x, https://x.com/?method=m&sk=REDACTED&api_key=REDACTED&api_sig=REDACTED",
		"https://x.com/?CLIENT_SECRET=s&code=c, https://x.com/?CLIENT_SECRET=REDACTED&code=REDACTED",
		"https://x.com/?refresh_token=$1#x, https://x.com/?refresh_token=REDACTED#x",
	})
	public void testRedactsCredentials(String url, String expected) {
		assertThat(Urls.redact(url)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://x.com/?action=get&startdate=1", "https://x.com/token", "https://x.com/?task" })
	public void testKeepsOtherUrls(String url) {
		assertThat(Urls.redact(url)).isEqualTo(url);
	}
}
