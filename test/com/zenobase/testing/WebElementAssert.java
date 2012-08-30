package com.zenobase.testing;

import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import org.openqa.selenium.WebElement;

public class WebElementAssert extends GenericAssert<WebElementAssert, WebElement> {

	private WebElementAssert(WebElement actual) {
		super(WebElementAssert.class, actual);
	}

	public static WebElementAssert assertThat(WebElement actual) {
		return new WebElementAssert(actual);
	}

	public WebElementAssert isDisplayed() {
		return isDisplayed(true);
	}

	public WebElementAssert isNotDisplayed() {
		return isDisplayed(false);
	}

	private WebElementAssert isDisplayed(boolean expected) {
		Assertions.assertThat(actual.isDisplayed()).as(actual + " is displayed").isEqualTo(expected);
		return this;
	}

	public WebElementAssert isEnabled() {
		return isEnabled(true);
	}

	public WebElementAssert isNotEnabled() {
		return isEnabled(false);
	}

	private WebElementAssert isEnabled(boolean expected) {
		Assertions.assertThat(actual.isEnabled()).as(actual + " is enabled").isEqualTo(expected);
		return this;
	}

	public WebElementAssert hasText(String expected) {
		Assertions.assertThat(actual.getText()).as("text of " + actual).isEqualTo(expected);
		return this;
	}
}
