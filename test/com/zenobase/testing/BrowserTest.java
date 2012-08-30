package com.zenobase.testing;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withText;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;

import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import play.libs.WS;
import play.test.TestBrowser;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Injector;

import com.zenobase.common.Globals;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.UserRepository;

public class BrowserTest {

	private static final int PORT = 9000;

	@Test
	//@Ignore
	public void testStory() {
		running(testServer(PORT), FIREFOX, new play.libs.F.Callback<TestBrowser>() {
			@Override
			public void invoke(TestBrowser browser) throws Throwable {

				Injector injector = Globals.get(Injector.class);
				UserRepository users = injector.getInstance(UserRepository.class);
				BucketRepository buckets = injector.getInstance(BucketRepository.class);

				// open home page
				browser.goTo("http://localhost:" + PORT);
				assertThat(browser.title()).isEqualTo("Zenobase");
				browser.await().atMost(5, TimeUnit.SECONDS).until("#home-view").isPresent();
				assertThat(browser.findFirst("#sign-up-banner")).as("sign up banner").isNotDisplayed();
				assertThat(browser.findFirst("#alert-banner")).as("alert banner").isNotDisplayed();
				assertThat(browser.findFirst("#sign-in-link")).as("sign in link").isDisplayed();
				assertThat(browser.findFirst("#sign-out-link")).as("sign out link").isNotDisplayed();
				assertThat(browser.findFirst("#user-profile-link")).as("user profile link").isNotDisplayed();
				assertThat(browser.findFirst("#existing-user-link")).as("existing user link").isNotDisplayed();

				// follow get started link
				browser.findFirst("#new-user-link").click();
				sleep(); // browser.await().atMost(5, TimeUnit.SECONDS).until("#dashboard-view").isPresent();
				assertThat(browser.findFirst("#sign-up-banner")).as("sign up banner").isDisplayed();
				assertThat(browser.findFirst("#alert-banner")).as("alert banner").isNotDisplayed();
				assertThat(browser.findFirst("#sign-in-link")).as("sign in link").isNotDisplayed();
				assertThat(browser.findFirst("#sign-out-link")).as("sign out link").isDisplayed();
				assertThat(browser.findFirst("#user-profile-link").getText()).as("user profile link").isEqualTo("guest");
				assertThat(browser.findFirst("#dashboard-message")).as("dashboard message").isNotDisplayed();
				assertThat(browser.findFirst("#dashboard-loading-message")).as("dashboard loading message").isNotDisplayed();
				assertThat(browser.findFirst("#edit-event-dialog")).as("edit event dialog").isDisplayed();
				assertThat(buckets.findBuckets(0, 1).size()).as("number of buckets").isEqualTo(1L);

				// add a timestamp to the event
				browser.findFirst("#event-field-select option", withText("timestamp")).click();
				browser.fill("#timestamp-value-field").with("foo");
				assertThat(browser.findFirst("#add-timestamp-button")).as("add timestamp button").isNotEnabled();
				browser.fill("#timestamp-value-field").with("2012-08-04T08:30:00.000-0700");
				browser.click("#add-timestamp-button");
				assertThat(browser.findFirst("#save-event-button")).as("save event button").isEnabled();

				// add a tag to the event
				browser.findFirst("#event-field-select option", withText("tag")).click();
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isNotEnabled();
				browser.fill("#tag-value-field").with("hike");
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isEnabled();
				browser.click("#add-tag-button");
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isNotEnabled();

				// add a location
				browser.findFirst("#event-field-select option", withText("location")).click();
				assertThat(browser.findFirst("#add-location-button")).as("add location button").isEnabled();
				browser.click("#add-location-button");

				// add a distance
				browser.findFirst("#event-field-select option", withText("distance")).click();
				assertThat(browser.findFirst("#add-distance-button")).as("add distance button").isNotEnabled();
				browser.fill("#distance-value-field").with("12.2");
				assertThat(browser.findFirst("#add-distance-button")).as("add distance button").isNotEnabled();
				browser.findFirst("#distance-unit-select option", withText("mi")).click();
				assertThat(browser.findFirst("#add-distance-button")).as("add distance button").isEnabled();
				browser.click("#add-distance-button");
				assertThat(browser.findFirst("#add-distance-button")).as("add distance button").isNotEnabled();

				// add a height
				browser.findFirst("#event-field-select option", withText("height")).click();
				assertThat(browser.findFirst("#add-height-button")).as("add height button").isNotEnabled();
				browser.fill("#height-value-field").with("6970");
				assertThat(browser.findFirst("#add-height-button")).as("add height button").isNotEnabled();
				browser.findFirst("#height-unit-select option", withText("ft")).click();
				assertThat(browser.findFirst("#add-height-button")).as("add height button").isEnabled();
				browser.click("#add-height-button");
				assertThat(browser.findFirst("#add-height-button")).as("add height button").isNotEnabled();

				// add a resource
				browser.findFirst("#event-field-select option", withText("resource")).click();
				assertThat(browser.findFirst("#add-resource-button")).as("add resource button").isNotEnabled();
				browser.fill("#resource-url-field").with("http://picasaweb.google.com/eric.jain/MountAdamsAugust2012");
				assertThat(browser.findFirst("#add-resource-button")).as("add resource button").isNotEnabled();
				browser.fill("#resource-title-field").with("Mount Adams");
				assertThat(browser.findFirst("#add-resource-button")).as("add resource button").isEnabled();
				browser.click("#add-resource-button");
				assertThat(browser.findFirst("#add-resource-button")).as("add resource button").isNotEnabled();

				// add a duration
				browser.findFirst("#event-field-select option", withText("duration")).click();
				assertThat(browser.findFirst("#add-duration-button")).as("add duration button").isNotEnabled();
				browser.fill("#duration-hours-field").with("19");
				browser.fill("#duration-minutes-field").with("30");
				assertThat(browser.findFirst("#add-duration-button")).as("add duration button").isEnabled();
				browser.click("#add-duration-button");
				assertThat(browser.findFirst("#add-duration-button")).as("add duration button").isNotEnabled();

				// add a note
				browser.findFirst("#event-field-select option", withText("note")).click();
				assertThat(browser.findFirst("#add-note-button")).as("add note button").isNotEnabled();
				browser.fill("#note-value-field").with("nice views");
				assertThat(browser.findFirst("#add-note-button")).as("add note button").isEnabled();
				browser.click("#add-note-button");
				assertThat(browser.findFirst("#add-note-button")).as("add note button").isNotEnabled();

				// add a rating
				browser.findFirst("#event-field-select option", withText("rating")).click();
				browser.fill("#rating-value-field").with("80");
				assertThat(browser.findFirst("#add-rating-button")).as("add rating button").isEnabled();
				browser.click("#add-rating-button");

				browser.click("#save-event-button");
				sleep();

				assertThat(browser.findFirst("#event-count").getText()).as("event count").isEqualTo("1");

				WebElement test = browser.getDefaultDriver().findElement(By.id("event-count"));
				assertThat(test.isDisplayed()).isTrue();

				// WebElement row = browser.getDefaultDriver().findElement(By.className("event-row"));
				// WebElement action = browser.getDefaultDriver().findElement(By.className("event-delete-action"));
				// new Actions(browser.getDefaultDriver()).moveToElement(row).click(action).perform();
				// browser.click("td[id^='event-']");
				// browser.click("a[id^='edit-event-']");

				// edit event: add another tag, delete a field

				// add benchpress and weather event

				// add events programmatically, refresh

				// import/export

				// add & configure widgets

				// test filtering & paging

				// sign up
				browser.click("#sign-up-link");
				assertThat(browser.findFirst("#sign-up-dialog")).as("sign up dialog").isDisplayed();
				assertThat(browser.findFirst("#sign-up-message")).as("sign up message").isNotDisplayed();
				assertThat(browser.findFirst("#sign-up-button")).as("sign up button").isNotEnabled();
				browser.fill("#sign-up-username").with("jdoe");
				assertThat(browser.findFirst("#sign-up-button")).as("sign up button").isNotEnabled();
				browser.fill("#sign-up-password").with("password123");
				browser.fill("#sign-up-password-repeat").with("password");
				assertThat(browser.findFirst("#sign-up-button")).as("sign up button").isNotEnabled();
				browser.fill("#sign-up-email").with("jdoe@zenobase.com");
				assertThat(browser.findFirst("#sign-up-button")).as("sign up button").isEnabled();
				browser.click("#sign-up-button");
				assertThat(browser.findFirst("#sign-up-dialog")).as("sign up dialog").isDisplayed();
				assertThat(browser.findFirst("#sign-up-message")).as("sign up message").isDisplayed();
				browser.fill("#sign-up-password-repeat").with("password123");
				browser.click("#sign-up-button");
				sleep();

				assertThat(browser.findFirst("#user-title").getText()).as("title with user name").isEqualTo("jdoe");
				assertThat(browser.find(".bucket-link")).as("bucket link").hasSize(1);
				assertThat(browser.findFirst("#sign-up-banner")).as("sign up banner").isNotDisplayed();
				assertThat(browser.findFirst("#alert-banner")).as("alert banner").isNotDisplayed();
				assertThat(browser.findFirst("#sign-in-link")).as("sign in link").isNotDisplayed();
				assertThat(browser.findFirst("#sign-out-link")).as("sign out link").isDisplayed();
				assertThat(browser.findFirst("#user-profile-link").getText()).as("user profile link").isEqualTo("jdoe");

				List<Message> inbox = Mailbox.get("jdoe@zenobase.com");
				assertThat(inbox).as("messages").hasSize(1);
				Message m = Iterables.getOnlyElement(inbox);
				browser.goTo(extractUrl(m.getContent().toString()));
				sleep(); // browser.await().atMost(5, TimeUnit.SECONDS).until("#user-view").isPresent();

				assertThat(users.find("jdoe").isVerified()).as("user is verified").isTrue();

				// edit profile -> change email
				// delete bucket & undo
				// add bucket
				// add buckets programmatically

				Identity identity = users.find("jdoe").asIdentity();
				for (int i = 0; i < 5; ++i) {
					Bucket b = new Bucket();
					b.addPermission(identity, Permission.ALL);
					b.setLabel("Bucket #" + i);
					buckets.store(b, true);
				}
				// refresh
				assertThat(browser.findFirst("#prev-buckets-button")).as("prev buckets button").isNotEnabled();
				assertThat(browser.findFirst("#next-buckets-button")).as("next buckets button").isNotEnabled();
				browser.click("#refresh-buckets-link");
				sleep();
				// test paging
				assertThat(browser.findFirst("#prev-buckets-button")).as("prev buckets button").isNotEnabled();
				assertThat(browser.findFirst("#next-buckets-button")).as("next buckets button").isEnabled();
				browser.click("#next-buckets-button");
				sleep();
				assertThat(browser.findFirst("#prev-buckets-button")).as("prev buckets button").isEnabled();
				assertThat(browser.findFirst("#next-buckets-button")).as("next buckets button").isNotEnabled();

				// log back out
				// access as guest
				// log in
				// publish bucket
				// log out
				// access as guest
				// log back in

				// close account
				browser.executeScript("window.confirm=function(){return true;}");
				browser.click("#edit-user-link");
				browser.click("#close-account-button");
				sleep();
				assertThat(browser.findFirst("#sign-up-banner")).as("sign up banner").isNotDisplayed();
				assertThat(browser.findFirst("#alert-banner")).as("alert banner").isNotDisplayed();
				assertThat(browser.findFirst("#sign-in-link")).as("sign in link").isDisplayed();
				assertThat(browser.findFirst("#sign-out-link")).as("sign out link").isNotDisplayed();
				assertThat(browser.findFirst("#user-profile-link")).as("user profile link").isNotDisplayed();
				assertThat(browser.findFirst("#existing-user-link")).as("existing user link").isNotDisplayed();
				assertThat(users.find("jdoe").isSuspended()).as("user is suspended").isTrue();

				browser.quit();
			}
		});
	}

	private static void sleep() {
		sleep(3);
	}

	private static void sleep(int seconds) {
		Uninterruptibles.sleepUninterruptibly(seconds, TimeUnit.SECONDS);
	}

	private static String extractUrl(String message) {
		Pattern p = Pattern.compile("http\\S+");
		Matcher matcher = p.matcher(message);
		assertThat(matcher.find()).as("contains a link: " + message).isTrue();
		return matcher.group(0);
	}

	@Test
	@Ignore
	public void testStatus() {
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				assertThat(WS.url(String.format("http://localhost:%d/status", PORT)).get().get().getStatus()).isEqualTo(OK);
			}
		});
	}
}
