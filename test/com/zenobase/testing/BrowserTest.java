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

				// add a timestamp to the event
				browser.findFirst("#event-field-select option", withText("timestamp")).click();
				assertThat(browser.findFirst("#save-event-button")).as("save event button").isNotEnabled();
				browser.fill("#event-timestamp-field").with("foo");
				assertThat(browser.findFirst("#add-timestamp-button")).as("add timestamp button").isNotEnabled();
				browser.fill("#event-timestamp-field").with("2012-03-31T23:00:00.000-0700");
				browser.click("#add-timestamp-button");
				assertThat(browser.findFirst("#save-event-button")).as("save event button").isEnabled();

				// add a tag to the event
				browser.findFirst("#event-field-select option", withText("tag")).click();
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isNotEnabled();
				browser.fill("#event-tag-field").with("foo");
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isEnabled();
				browser.click("#add-tag-button");
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isNotEnabled();

				// add a second tag to the event
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isNotEnabled();
				browser.fill("#event-tag-field").with("bar");
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isEnabled();
				browser.click("#add-tag-button");
				assertThat(browser.findFirst("#add-tag-button")).as("add tag button").isNotEnabled();

				browser.click("#save-event-button");
				sleep();

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
				browser.await().atMost(5, TimeUnit.SECONDS).until("#user-view").isPresent();

				// add & configure widgets
				// test filtering & paging

				// assert is verified
				// edit profile -> change email
				// delete bucket & undo
				// add bucket
				// add buckets programmatically
				assertThat(users.find("jdoe").isVerified()).as("user is verified").isTrue();

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
