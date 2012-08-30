package com.zenobase.testing;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
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

	@Before
	public void setUp() {
		Assume.assumeNotNull(System.getProperty("play.version"));
		try {
			driver = new ChromeDriver();
		} catch (IllegalStateException e) {
			Assume.assumeNoException(e);
		}
	}

	@Test
	public void test() {
		running(testServer(PORT), new Runnable() {

			@Override
			public void run() {

				Injector injector = Globals.get(Injector.class);
				UserRepository users = injector.getInstance(UserRepository.class);
				BucketRepository buckets = injector.getInstance(BucketRepository.class);

				driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
				WebDriverWait wait = new WebDriverWait(driver, 5);

				// open home page
				driver.get("http://localhost:" + PORT);
				assertThat(driver.getTitle()).isEqualTo("Zenobase");
				wait.withMessage("home view is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("home-view")));
				assertThat(driver.findElement(By.id("sign-up-banner")).isDisplayed()).as("sign up banner").isFalse();
				assertThat(driver.findElement(By.id("alert-banner")).isDisplayed()).as("alert banner").isFalse();
				assertThat(driver.findElement(By.id("sign-in-link")).isDisplayed()).as("sign in link").isTrue();
				assertThat(driver.findElement(By.id("sign-out-link")).isDisplayed()).as("sign out link").isFalse();
				assertThat(driver.findElement(By.id("user-profile-link")).isDisplayed()).as("user profile link").isFalse();
				assertThat(driver.findElement(By.id("existing-user-link")).isDisplayed()).as("existing user link").isFalse();
				assertThat(buckets.findBuckets(0, 1).size()).as("number of buckets").isEqualTo(0L);

				// follow get started link
				driver.findElement(By.id("new-user-link")).click();
				wait.withMessage("edit event dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-event-dialog")));
				assertThat(driver.findElement(By.id("sign-up-banner")).isDisplayed()).as("sign up banner is displayed").isTrue();
				assertThat(driver.findElement(By.id("alert-banner")).isDisplayed()).as("alert banner is displayed").isFalse();
				assertThat(driver.findElement(By.id("sign-in-link")).isDisplayed()).as("sign in link is displayed").isFalse();
				assertThat(driver.findElement(By.id("sign-out-link")).isDisplayed()).as("sign out link is displayed").isTrue();
				assertThat(driver.findElement(By.id("user-profile-link")).getText()).as("user profile link text").isEqualTo("guest");
				assertThat(driver.findElement(By.id("dashboard-message")).isDisplayed()).as("dashboard message is displayed").isFalse();
				assertThat(driver.findElement(By.id("dashboard-loading-message")).isDisplayed()).as("dashboard loading message is displayed").isFalse();
				assertThat(buckets.findBuckets(0, 1).size()).as("number of buckets").isEqualTo(1L);

				// add a timestamp to the event
				Select eventFieldSelect = new Select(driver.findElement(By.id("event-field-select")));
				WebElement saveEventButton = driver.findElement(By.id("save-event-button"));
				eventFieldSelect.selectByVisibleText("timestamp");
				WebElement timestampValueField = driver.findElement(By.id("timestamp-value-field"));
				WebElement addTimestampButton = driver.findElement(By.id("add-timestamp-button"));
				timestampValueField.clear();
				timestampValueField.sendKeys("foo");
				assertThat(addTimestampButton.isEnabled()).as("add timestamp button is enabled").isFalse();
				timestampValueField.clear();
				timestampValueField.sendKeys("2012-08-04T08:30:00.000-0700");
				addTimestampButton.click();
				assertThat(saveEventButton.isEnabled()).as("save event button is enabled").isTrue();

				// add a tag to the event
				eventFieldSelect.selectByVisibleText("tag");
				WebElement addTagButton = driver.findElement(By.id("add-tag-button"));
				WebElement tagValueField = driver.findElement(By.id("tag-value-field"));
				assertThat(addTagButton.isEnabled()).as("add tag button is enabled").isFalse();
				tagValueField.sendKeys("hike");
				assertThat(addTagButton.isEnabled()).as("add tag button is enabled").isTrue();
				addTagButton.click();
				assertThat(addTagButton.isEnabled()).as("add tag button is enabled").isFalse();

				// add a location
				eventFieldSelect.selectByVisibleText("location");
				WebElement addLocationButton = driver.findElement(By.id("add-location-button"));
				assertThat(addLocationButton.isEnabled()).as("add location button is enabled").isFalse();
				wait.withMessage("add location button is enabled").until(ExpectedConditions.elementToBeClickable(By.id("add-location-button")));
				addLocationButton.click();

				// add a distance
				eventFieldSelect.selectByVisibleText("distance");
				WebElement addDistanceButton = driver.findElement(By.id("add-distance-button"));
				assertThat(addDistanceButton.isEnabled()).as("add distance button is enabled").isFalse();
				driver.findElement(By.id("distance-value-field")).sendKeys("12.2");
				assertThat(addDistanceButton.isEnabled()).as("add distance button is enabled").isFalse();
				new Select(driver.findElement(By.id("distance-unit-select"))).selectByVisibleText("mi");
				assertThat(addDistanceButton.isEnabled()).as("add distance button is enabled").isTrue();
				addDistanceButton.click();
				assertThat(addDistanceButton.isEnabled()).as("add distance button is enabled").isFalse();

				// add a height
				eventFieldSelect.selectByVisibleText("height");
				WebElement addHeightButton = driver.findElement(By.id("add-height-button"));
				assertThat(addHeightButton.isEnabled()).as("add height button is enabled").isFalse();
				driver.findElement(By.id("height-value-field")).sendKeys("6970");
				assertThat(addHeightButton.isEnabled()).as("add height button is enabled").isFalse();
				new Select(driver.findElement(By.id("height-unit-select"))).selectByVisibleText("ft");
				assertThat(addHeightButton.isEnabled()).as("add height button is enabled").isTrue();
				addHeightButton.click();
				assertThat(addHeightButton.isEnabled()).as("add height button is enabled").isFalse();

				// add a resource
				eventFieldSelect.selectByVisibleText("resource");
				WebElement addResourceButton = driver.findElement(By.id("add-resource-button"));
				assertThat(addResourceButton.isEnabled()).as("add resource button is enabled").isFalse();
				driver.findElement(By.id("resource-url-field")).sendKeys("http://picasaweb.google.com/eric.jain/MountAdamsAugust2012");
				assertThat(addResourceButton.isEnabled()).as("add resource button is enabled").isFalse();
				driver.findElement(By.id("resource-title-field")).sendKeys("Mount Adams");
				assertThat(addResourceButton.isEnabled()).as("add resource button is enabled").isTrue();
				addResourceButton.click();
				assertThat(addResourceButton.isEnabled()).as("add resource button is enabled").isFalse();

				// add a duration
				eventFieldSelect.selectByVisibleText("duration");
				WebElement addDurationButton = driver.findElement(By.id("add-duration-button"));
				assertThat(addDurationButton.isEnabled()).as("add duration button is enabled").isFalse();
				driver.findElement(By.id("duration-hours-field")).sendKeys("19");
				driver.findElement(By.id("duration-minutes-field")).sendKeys("30");
				assertThat(addDurationButton.isEnabled()).as("add duration button is enabled").isTrue();
				addDurationButton.click();
				assertThat(addDurationButton.isEnabled()).as("add duration button is enabled").isFalse();

				// add a note
				eventFieldSelect.selectByVisibleText("note");
				WebElement addNoteButton = driver.findElement(By.id("add-note-button"));
				assertThat(addNoteButton.isEnabled()).as("add note button is enabled").isFalse();
				driver.findElement(By.id("note-value-field")).sendKeys("nice views");
				assertThat(addNoteButton.isEnabled()).as("add note button is enabled").isTrue();
				addNoteButton.click();
				assertThat(addNoteButton.isEnabled()).as("add note button is enabled").isFalse();

				// add a rating
				eventFieldSelect.selectByVisibleText("rating");
				WebElement addRatingButton = driver.findElement(By.id("add-rating-button"));
				driver.findElement(By.id("rating-value-field")).sendKeys("80");
				assertThat(addRatingButton.isEnabled()).as("add rating button is enabled").isTrue();
				addRatingButton.click();

				saveEventButton.click();

				wait.withMessage("event count").until(ExpectedConditions.textToBePresentInElement(By.id("event-count"), "1"));
				WebElement row = driver.findElement(By.className("event-row"));
				WebElement action = driver.findElement(By.className("event-edit-action"));
				new Actions(driver).moveToElement(row).click(action).perform();

				wait.withMessage("edit event dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-event-dialog")));
				WebElement cancelEventButton = driver.findElement(By.id("cancel-event-button"));
				cancelEventButton.click();


				// edit event: add another tag, delete a field

				// add benchpress and weather event

				// add events programmatically, refresh

				// import/export

				// add & configure widgets

				// test filtering & paging


				// sign up
				driver.findElement(By.id("sign-up-link")).click();
				assertThat(driver.findElement(By.id("sign-up-dialog")).isDisplayed()).as("sign up dialog is displayed").isTrue();
				assertThat(driver.findElement(By.id("sign-up-message")).isDisplayed()).as("sign up message is displayed").isFalse();
				WebElement signUpButton = driver.findElement(By.id("sign-up-button"));
				assertThat(signUpButton.isEnabled()).as("sign up button is enabled").isFalse();
				driver.findElement(By.id("sign-up-username")).sendKeys("jdoe");
				assertThat(signUpButton.isEnabled()).as("sign up button is enabled").isFalse();
				driver.findElement(By.id("sign-up-password")).sendKeys("password123");
				driver.findElement(By.id("sign-up-password-repeat")).sendKeys("password");
				assertThat(signUpButton.isEnabled()).as("sign up button is enabled").isFalse();
				driver.findElement(By.id("sign-up-email")).sendKeys("jdoe@zenobase.com");
				assertThat(signUpButton.isEnabled()).as("sign up button is enabled").isTrue();
				signUpButton.click();
				assertThat(driver.findElement(By.id("sign-up-dialog")).isDisplayed()).as("sign up dialog is displayed").isTrue();
				assertThat(driver.findElement(By.id("sign-up-message")).isDisplayed()).as("sign up message is displayed").isTrue();
				driver.findElement(By.id("sign-up-password-repeat")).clear();
				driver.findElement(By.id("sign-up-password-repeat")).sendKeys("password123");
				signUpButton.click();

				wait.withMessage("view title equals user name").until(ExpectedConditions.textToBePresentInElement(By.id("user-title"), "jdoe"));
				assertThat(driver.findElements(By.className("bucket-link"))).as("bucket link").hasSize(1);
				assertThat(driver.findElement(By.id("sign-up-banner")).isDisplayed()).as("sign up banner is displayed").isFalse();
				assertThat(driver.findElement(By.id("alert-banner")).isDisplayed()).as("alert banner is displayed").isFalse();
				assertThat(driver.findElement(By.id("sign-in-link")).isDisplayed()).as("sign in link is displayed").isFalse();
				assertThat(driver.findElement(By.id("sign-out-link")).isDisplayed()).as("sign out link is displayed").isTrue();
				assertThat(driver.findElement(By.id("user-profile-link")).getText()).as("user profile link text").isEqualTo("jdoe");

				try {
					List<Message> inbox = Mailbox.get("jdoe@zenobase.com");
					assertThat(inbox).as("messages").hasSize(1);
					Message m = Iterables.getOnlyElement(inbox);
					driver.get(extractUrl(m.getContent().toString()));
					inbox.clear();
					wait.withMessage("view title equals user name").until(ExpectedConditions.textToBePresentInElement(By.id("user-title"), "jdoe"));
				} catch (IOException e) {
					throw new AssertionError(e);
				} catch (MessagingException e) {
					throw new AssertionError(e);
				}

				assertThat(users.find("jdoe").isVerified()).as("user is verified").isTrue();


				// edit profile -> change email

				// delete bucket & undo

				// add bucket


				Identity identity = users.find("jdoe").asIdentity();
				for (int i = 0; i < 5; ++i) {
					Bucket b = new Bucket();
					b.addPermission(identity, Permission.ALL);
					b.setLabel("Bucket #" + i);
					buckets.store(b, true);
				}

				// refresh
				WebElement prevBucketsButton = driver.findElement(By.id("prev-buckets-button"));
				WebElement nextBucketsButton = driver.findElement(By.id("next-buckets-button"));
				WebElement refreshBucketsLink = driver.findElement(By.id("refresh-buckets-link"));
				assertThat(prevBucketsButton.isEnabled()).as("prev buckets button is enabled when there is just one bucket").isFalse();
				assertThat(nextBucketsButton.isEnabled()).as("next buckets button is enabled when there is just one bucket").isFalse();
				refreshBucketsLink.click();

				// test paging
				nextBucketsButton = wait.withMessage("next buckets button is enabled before paging").until(ExpectedConditions.elementToBeClickable(By.id("next-buckets-button")));
				assertThat(driver.findElement(By.id("prev-buckets-button")).isEnabled()).as("prev buckets button is enabled before paging").isFalse();
				nextBucketsButton.click();

				prevBucketsButton = wait.withMessage("prev buckets button is enabled after paging").until(ExpectedConditions.elementToBeClickable(By.id("prev-buckets-button")));
				assertThat(driver.findElement(By.id("next-buckets-button")).isEnabled()).as("next buckets button is enabled after paging").isFalse();
				prevBucketsButton.click();


				// log back out

				// access as guest

				// log in

				// publish bucket

				// log out

				// access as guest

				// log back in


				// close account
				driver.findElement(By.id("edit-user-link")).click();
				driver.findElement(By.id("close-account-button")).click();
				driver.switchTo().alert().accept();

				wait.withMessage("home view is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("home-view")));
				assertThat(driver.findElement(By.id("sign-up-banner")).isDisplayed()).as("sign up banner is displayed").isFalse();
				assertThat(driver.findElement(By.id("alert-banner")).isDisplayed()).as("alert banner is displayed").isFalse();
				assertThat(driver.findElement(By.id("sign-in-link")).isDisplayed()).as("sign in link is displayed").isTrue();
				assertThat(driver.findElement(By.id("sign-out-link")).isDisplayed()).as("sign out link is displayed").isFalse();
				assertThat(driver.findElement(By.id("user-profile-link")).isDisplayed()).as("user profile link is displayed").isFalse();
				assertThat(driver.findElement(By.id("existing-user-link")).isDisplayed()).as("existing user link is displayed").isFalse();
				assertThat(users.find("jdoe").isSuspended()).as("user is suspended").isTrue();

				driver.quit();
			}
		});
	}

	private static String extractUrl(String message) {
		Pattern p = Pattern.compile("http\\S+");
		Matcher matcher = p.matcher(message);
		assertThat(matcher.find()).as("contains a link: " + message).isTrue();
		return matcher.group(0);
	}

	@After
	public void tearDown() {
		Mailbox.clearAll();
	}
}
