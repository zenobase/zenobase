package com.zenobase.testing;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withText;
import static play.test.Helpers.*;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import play.test.TestBrowser;

public class BrowserTest {

	@Test
	@Ignore
	public void test() {
		running(testServer(9000, fakeApplication()), FIREFOX, new play.libs.F.Callback<TestBrowser>() {
			@Override
			public void invoke(TestBrowser browser) throws Throwable {

				browser.goTo("http://localhost:9000");
				assertThat(browser.title()).isEqualTo("Zenobase");
				assertThat(browser.find("#content")).isNotEmpty();
				assertThat(browser.findFirst("a", withText().equalTo("Sign in"))).isDisplayed();
				assertThat(browser.find("a", withText().equalTo("Sign out"))).isEmpty();

				browser.await().until(".hero-unit").isPresent();
				assertThat(browser.find("a", withText().startsWith("Your Data"))).isEmpty();
				assertThat(browser.findFirst("a", withText().startsWith("Enter Data"))).isDisplayed();
				browser.findFirst("a", withText().startsWith("Enter Data")).click();
				browser.await().atMost(5, TimeUnit.SECONDS).until(".badge").isPresent();
				browser.quit();
			}
		});
	}
}
