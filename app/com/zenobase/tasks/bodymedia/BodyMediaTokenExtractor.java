package com.zenobase.tasks.bodymedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.TokenExtractorImpl;
import org.scribe.model.Token;
import org.scribe.utils.OAuthEncoder;

import com.zenobase.oauth.ExpiringToken;

public class BodyMediaTokenExtractor implements AccessTokenExtractor {

	private static final Pattern EXPIRATION_REGEX = Pattern.compile("xoauth_token_expiration=([^&]*)");

	@Override
	public ExpiringToken extract(String response) {
		Token token = new TokenExtractorImpl().extract(response);
		return new ExpiringToken(token.getToken(), token.getSecret(), getDateTime(extract(response, EXPIRATION_REGEX)), null);
	}

	private String extract(String response, Pattern p) {
		Matcher matcher = p.matcher(response);
		return matcher.find() ? OAuthEncoder.decode(matcher.group(1)) : null;
	}

	private DateTime getDateTime(String value) {
		return value != null ? new DateTime(Long.parseLong(value) * 1000, DateTimeZone.UTC) : null;
	}
}
