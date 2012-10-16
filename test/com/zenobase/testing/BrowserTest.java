package com.zenobase.testing;

import static com.zenobase.testing.WebElementAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
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

	private WebDriver driver;
	private WebDriverWait wait;

	@Before
	public void setUp() {
		Assume.assumeNotNull(System.getProperty("play.version"));
		try {
			driver = new ChromeDriver();
			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
			wait = new WebDriverWait(driver, 5);
		} catch (IllegalStateException e) {
			Assume.assumeNoException(e);
		}
	}

	@Test
	public void test() {
		running(testServer(PORT), new Runnable() {

			@Inject
			UserRepository users;

			@Inject
			BucketRepository buckets;

			@Override
			public void run() {

				Globals.get(Injector.class).injectMembers(this);

				// home
				driver.get("http://localhost:" + PORT);
				assertThat(driver.getTitle()).isEqualTo("Zenobase");
				wait.withMessage("home view is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("home-view")));
				assertThat($("#sign-up-banner")).isNotDisplayed();
				assertThat($("#alert-banner")).isNotDisplayed();
				assertThat($("#sign-in-link")).isDisplayed();
				assertThat($("#sign-out-link")).isNotDisplayed();
				assertThat($("#user-profile-link")).isNotDisplayed();
				assertThat($("#existing-user-link")).isNotDisplayed();
				assertThat(buckets.findBuckets(0, 1).size()).as("number of buckets").isEqualTo(0L);

				// follow get started link
				$("#new-user-link").click();
				wait.withMessage("edit event dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-event-dialog")));
				assertThat($("#sign-up-banner")).isDisplayed();
				assertThat($("#alert-banner")).isNotDisplayed();
				assertThat($("#sign-in-link")).isNotDisplayed();
				assertThat($("#sign-out-link")).isDisplayed();
				assertThat($("#user-profile-link")).hasText("guest");
				assertThat($("#dashboard-message")).isNotDisplayed();
				assertThat($("#dashboard-loading-message")).isNotDisplayed();
				assertThat($("#save-event-button")).isNotEnabled();
				assertThat(buckets.findBuckets(0, 1).size()).as("number of buckets").isEqualTo(1L);
				String privateBucketUrl = driver.getCurrentUrl();

				// add a timestamp to the event
				new Select($("#event-field-select")).selectByVisibleText("timestamp");
				$("#timestamp-date-field").clear();
				$("#timestamp-date-field").sendKeys("2012-08-04");
				$("#timestamp-time-field").click();
				$("#timestamp-time-field").sendKeys("08");
				$("#timestamp-time-field").sendKeys("\t");
				$("#timestamp-time-field").sendKeys("30");
				new Select($("#timestamp-timezone-select")).selectByVisibleText("-07:00");
				$("#add-timestamp-button").click();
				assertThat($("#save-event-button")).isEnabled();

				// add a tag to the event
				new Select($("#event-field-select")).selectByVisibleText("tag");
				assertThat($("#add-tag-button")).isNotEnabled();
				$("#tag-value-field").sendKeys("hike");
				assertThat($("#add-tag-button")).isEnabled();
				$("#add-tag-button").click();
				assertThat($("#add-tag-button")).isNotEnabled();

				// add a location
				new Select($("#event-field-select")).selectByVisibleText("location");
				assertThat($("#add-location-button")).isNotEnabled();
				wait.withMessage("add location button is enabled").until(ExpectedConditions.elementToBeClickable(By.id("add-location-button")));
				$("#add-location-button").click();

				// add a distance
				new Select($("#event-field-select")).selectByVisibleText("distance");
				assertThat($("#add-distance-button")).isNotEnabled();
				$("#distance-value-field").sendKeys("12.2");
				assertThat($("#add-distance-button")).isNotEnabled();
				new Select($("#distance-unit-select")).selectByVisibleText("mi");
				assertThat($("#add-distance-button")).isEnabled();
				$("#add-distance-button").click();
				assertThat($("#add-distance-button")).isNotEnabled();

				// add a height
				new Select($("#event-field-select")).selectByVisibleText("height");
				assertThat($("#add-height-button")).isNotEnabled();
				$("#height-value-field").sendKeys("6970");
				assertThat($("#add-height-button")).isNotEnabled();
				new Select($("#height-unit-select")).selectByVisibleText("ft");
				assertThat($("#add-height-button")).isEnabled();
				$("#add-height-button").click();
				assertThat($("#add-height-button")).isNotEnabled();

				// add a resource
				new Select($("#event-field-select")).selectByVisibleText("resource");
				assertThat($("#add-resource-button")).isNotEnabled();
				$("#resource-url-field").sendKeys("http://picasaweb.google.com/eric.jain/MountAdamsAugust2012");
				$("#resource-title-field").click();
				wait.withMessage("title is auto-filled").until(ExpectedConditions.elementToBeClickable(By.id("add-resource-button")));
				$("#add-resource-button").click();
				assertThat($("#add-resource-button")).isNotEnabled();

				// add a duration
				new Select($("#event-field-select")).selectByVisibleText("duration");
				assertThat($("#add-duration-button")).isNotEnabled();
				$("#duration-hours-field").sendKeys("19");
				$("#duration-minutes-field").sendKeys("30");
				assertThat($("#add-duration-button")).isEnabled();
				$("#add-duration-button").click();
				assertThat($("#add-duration-button")).isNotEnabled();

				// add a note
				new Select($("#event-field-select")).selectByVisibleText("note");
				assertThat($("#add-note-button")).isNotEnabled();
				$("#note-value-field").sendKeys("nice views");
				assertThat($("#add-note-button")).isEnabled();
				$("#add-note-button").click();
				assertThat($("#add-note-button")).isNotEnabled();

				// add a rating
				new Select($("#event-field-select")).selectByVisibleText("rating");
				$("#rating-4-star").click();
				$("#add-rating-button").click();

				$("#save-event-button").click();

				wait.withMessage("event count").until(ExpectedConditions.textToBePresentInElement(By.id("event-count"), "1"));

				// edit event
				new Actions(driver).moveToElement($(".event-row")).click($(".event-edit-action")).perform();
				wait.withMessage("edit event dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-event-dialog")));
				// TODO: add another tag, delete a field
				$("#cancel-event-button").click();

				// TODO: add benchpress and weather event

				// TODO: add events programmatically, refresh

				// TODO: import/export

				// TODO: add & configure widgets

				// TODO: test filtering & paging

				// sign up
				$("#sign-up-link").click();
				assertThat($("#sign-up-dialog")).isDisplayed();
				assertThat($("#sign-up-message")).isNotDisplayed();
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-username").sendKeys("jdoe");
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-password").sendKeys("password123");
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-password-confirm").sendKeys("password");
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-email").sendKeys("jdoe@zenobase.com");
				assertThat($("#sign-up-button")).isEnabled();
				$("#sign-up-button").click();
				assertThat($("#sign-up-dialog")).isDisplayed();
				assertThat($("#sign-up-message")).isDisplayed();
				$("#sign-up-password-confirm").sendKeys("123");
				$("#sign-up-button").click();
				wait.withMessage("view title equals user name").until(ExpectedConditions.textToBePresentInElement(By.id("user-title"), "jdoe"));
				assertThat(find(".bucket-link")).as("bucket links").hasSize(1);
				assertThat($("#sign-up-banner")).isNotDisplayed();
				assertThat($("#alert-banner")).isNotDisplayed();
				assertThat($("#sign-in-link")).isNotDisplayed();
				assertThat($("#sign-out-link")).isDisplayed();
				assertThat($("#user-profile-link")).hasText("jdoe");

				// TODO: edit profile -> change email

				// verify email
				driver.get(findUrl(readMessage("jdoe@zenobase.com")));
				wait.withMessage("view title equals user name").until(ExpectedConditions.textToBePresentInElement(By.id("user-title"), "jdoe"));
				assertThat(users.find("jdoe").isVerified()).as("user is verified").isTrue();

				// delete bucket and undo
				new Actions(driver).moveToElement($(".bucket-row")).click($(".bucket-delete-action")).perform();
				wait.withMessage("alert banner").until(ExpectedConditions.visibilityOfElementLocated(By.id("alert-banner")));
				assertThat(buckets.findBuckets(0, 0).size()).as("number of buckets").isEqualTo(0L);
				$("#undo-link").click();
				wait.withMessage("bucket").until(ExpectedConditions.visibilityOfElementLocated(By.className("bucket-row")));
				assertThat($("#alert-banner")).isNotDisplayed();
				assertThat(buckets.findBuckets(0, 0).size()).as("number of buckets").isEqualTo(1L);

				// generate and browse buckets
				createBuckets(5, users.find("jdoe").asIdentity(), buckets);
				assertThat($("#prev-buckets-button")).isNotEnabled();
				assertThat($("#next-buckets-button")).isNotEnabled();
				$("#refresh-buckets-link").click();
				wait.withMessage("next buckets button is enabled before paging").until(ExpectedConditions.elementToBeClickable(By.id("next-buckets-button")));
				assertThat($("#prev-buckets-button")).isNotEnabled();
				$("#next-buckets-button").click();
				wait.withMessage("prev buckets button is enabled after paging").until(ExpectedConditions.elementToBeClickable(By.id("prev-buckets-button")));
				assertThat($("#next-buckets-button")).isNotEnabled();
				$("#prev-buckets-button").click();

				// create a public bucket
				$("#add-bucket-action").click();
				assertThat($("#create-bucket-dialog")).isDisplayed();
				$("#create-bucket-label").clear();
				$("#create-bucket-label").sendKeys("Test Data");
				assertThat($("#create-bucket-button")).isEnabled();
				$("#create-bucket-button").click();
				wait.withMessage("edit event dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-event-dialog")));
				new Select($("#event-field-select")).selectByVisibleText("timestamp");
				$("#add-timestamp-button").click();
				$("#save-event-button").click();
				wait.withMessage("edit event dialog is not displayed").until(ExpectedConditions.invisibilityOfElementLocated(By.id("edit-event-dialog")));
				assertThat($("#bucket-title")).hasText("Test Data");
				$("#edit-bucket-action").click();
				$("#edit-bucket-label").clear();
				$("#edit-bucket-label").sendKeys("Public Data");
				assertThat($("#bucket-private-label")).isDisplayed();
				assertThat($("#bucket-public-label")).isNotDisplayed();
				assertThat($("#permissions-dialog-link")).isDisplayed();
				$("#permissions-dialog-link").click();
				assertThat($("#permissions-dialog")).isDisplayed();
				$("#publish-link").click();
				$("#update-permissions-button").click();
				assertThat($("#permissions-dialog")).isNotDisplayed();
				assertThat($("#bucket-private-label")).isNotDisplayed();
				assertThat($("#bucket-public-label")).isDisplayed();
				assertThat($("#save-bucket-button")).isEnabled();
				$("#save-bucket-button").click();
				wait.withMessage("bucket is displayed").until(ExpectedConditions.textToBePresentInElement(By.id("bucket-title"), "Public Data"));
				String publicBucketUrl = driver.getCurrentUrl();

				// access public bucket after signing out
				$("#sign-out-link").click();
				wait.withMessage("sign in link is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("sign-in-link")));
				driver.get(publicBucketUrl);
				wait.withMessage("bucket is displayed").until(ExpectedConditions.textToBePresentInElement(By.id("bucket-title"), "Public Data"));

				// try to access private bucket after signing out
				driver.get(privateBucketUrl);
				wait.withMessage("sign in dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("sign-in-dialog")));

				// try to log back in, reset password
				assertThat($("#sign-in-button")).isNotEnabled();
				$("#sign-in-username").sendKeys("jdoe");
				assertThat($("#sign-in-button")).isNotEnabled();
				$("#sign-in-password").sendKeys("????????");
				assertThat($("#sign-in-button")).isEnabled();
				$("#sign-in-button").click();
				wait.withMessage("sign in message is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("sign-in-message")));
				$("#password-reset-request-link").click();
				assertThat($("#password-reset-request-dialog")).isDisplayed();
				assertThat($("#password-reset-request-button")).isNotEnabled();
				$("#password-reset-request-username").sendKeys("jdoe");
				assertThat($("#password-reset-request-button")).isNotEnabled();
				$("#password-reset-request-email").sendKeys("foo@bar.baz");
				assertThat($("#password-reset-request-button")).isEnabled();
				$("#password-reset-request-button").click();
				wait.withMessage("password reset request message is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("password-reset-request-message")));
				assertThat($("#password-reset-request-dialog")).isDisplayed();
				$("#password-reset-request-email").clear();
				$("#password-reset-request-email").sendKeys("jdoe@zenobase.com");
				$("#password-reset-request-button").click();
				wait.withMessage("password reset confirmation is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("home-view")));
				assertThat(driver.getCurrentUrl()).as("page URL").endsWith("/#/");
				driver.get(findUrl(readMessage("jdoe@zenobase.com")));
				wait.withMessage("password reset view is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("password-reset-view")));
				assertThat($("#password-reset-button")).isNotEnabled();
				$("#password-reset-password").sendKeys("password123");
				assertThat($("#password-reset-button")).isNotEnabled();
				$("#password-reset-password-confirm").sendKeys("password");
				assertThat($("#password-reset-button")).isEnabled();
				$("#password-reset-button").click();
				assertThat($("#password-reset-message")).isDisplayed(); // passwords don't match
				$("#password-reset-password-confirm").sendKeys("123");
				$("#password-reset-button").click();

				// close account
				wait.withMessage("user profile link is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("user-profile-link")));
				$("#user-profile-link").click();
				wait.withMessage("user profile is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("user-view")));
				$("#edit-user-link").click();
				$("#close-account-button").click();
				driver.switchTo().alert().accept();
				wait.withMessage("home view is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("home-view")));
				assertThat($("#sign-up-banner")).isNotDisplayed();
				assertThat($("#alert-banner")).isNotDisplayed();
				assertThat($("#sign-in-link")).isDisplayed();
				assertThat($("#sign-out-link")).isNotDisplayed();
				assertThat($("#user-profile-link")).isNotDisplayed();
				assertThat($("#existing-user-link")).isNotDisplayed();
				assertThat(users.find("jdoe").isSuspended()).as("user is suspended").isTrue();
			}

			private String readMessage(String receipient) {
				try {
					return Iterables.getOnlyElement(Mailbox.get("jdoe@zenobase.com")).getContent().toString();
				} catch (IOException e) {
					throw new AssertionError(e);
				} catch (MessagingException e) {
					throw new AssertionError(e);
				} finally {
					Mailbox.clearAll();
				}
			}

			private String findUrl(String text) {
				Pattern p = Pattern.compile("http\\S+");
				Matcher matcher = p.matcher(text);
				assertThat(matcher.find()).as("contains a link: " + text).isTrue();
				return matcher.group(0);
			}

			private void createBuckets(int num, Identity owner, BucketRepository buckets) {
				for (int i = 0; i < num; ++i) {
					Bucket b = new Bucket();
					b.addPermission(owner, Permission.ALL);
					b.setLabel("Bucket #" + i);
					buckets.store(b, true);
				}
			}
		});
	}

	protected WebElement $(String selector) {
		return driver.findElement(By.cssSelector(selector));
	}

	protected List<WebElement> find(String selector) {
		return driver.findElements(By.cssSelector(selector));
	}

	protected void sleep(int seconds) {
		Uninterruptibles.sleepUninterruptibly(seconds, TimeUnit.SECONDS);
	}

	@After
	public void tearDown() {
		Mailbox.clearAll();
		if (driver != null) {
			driver.quit();
		}
	}
}
