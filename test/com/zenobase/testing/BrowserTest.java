package com.zenobase.testing;

import static com.zenobase.testing.WebElementAssert.assertThat;
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

				// add a timestamp to the event
				Select eventFieldSelect = new Select($("#event-field-select"));
				WebElement saveEventButton = $("#save-event-button");
				eventFieldSelect.selectByVisibleText("timestamp");
				WebElement timestampValueField = $("#timestamp-value-field");
				WebElement addTimestampButton = $("#add-timestamp-button");
				timestampValueField.clear();
				timestampValueField.sendKeys("foo");
				assertThat(addTimestampButton).isNotEnabled();
				timestampValueField.clear();
				timestampValueField.sendKeys("2012-08-04T08:30:00.000-0700");
				addTimestampButton.click();
				assertThat(saveEventButton).isEnabled();

				// add a tag to the event
				eventFieldSelect.selectByVisibleText("tag");
				WebElement addTagButton = $("#add-tag-button");
				WebElement tagValueField = $("#tag-value-field");
				assertThat(addTagButton).isNotEnabled();
				tagValueField.sendKeys("hike");
				assertThat(addTagButton).isEnabled();
				addTagButton.click();
				assertThat(addTagButton).isNotEnabled();

				// add a location
				eventFieldSelect.selectByVisibleText("location");
				assertThat($("#add-location-button")).isNotEnabled();
				wait.withMessage("add location button is enabled").until(ExpectedConditions.elementToBeClickable(By.id("add-location-button")));
				$("#add-location-button").click();

				// add a distance
				eventFieldSelect.selectByVisibleText("distance");
				WebElement addDistanceButton = $("#add-distance-button");
				assertThat(addDistanceButton).isNotEnabled();
				$("#distance-value-field").sendKeys("12.2");
				assertThat(addDistanceButton).isNotEnabled();
				new Select($("#distance-unit-select")).selectByVisibleText("mi");
				assertThat(addDistanceButton).isEnabled();
				addDistanceButton.click();
				assertThat(addDistanceButton).isNotEnabled();

				// add a height
				eventFieldSelect.selectByVisibleText("height");
				WebElement addHeightButton = $("#add-height-button");
				assertThat(addHeightButton).isNotEnabled();
				$("#height-value-field").sendKeys("6970");
				assertThat(addHeightButton).isNotEnabled();
				new Select($("#height-unit-select")).selectByVisibleText("ft");
				assertThat(addHeightButton).isEnabled();
				addHeightButton.click();
				assertThat(addHeightButton).isNotEnabled();

				// add a resource
				eventFieldSelect.selectByVisibleText("resource");
				WebElement addResourceButton = $("#add-resource-button");
				assertThat(addResourceButton).isNotEnabled();
				$("#resource-url-field").sendKeys("http://picasaweb.google.com/eric.jain/MountAdamsAugust2012");
				assertThat(addResourceButton).isNotEnabled();
				$("#resource-title-field").sendKeys("Mount Adams");
				assertThat(addResourceButton).isEnabled();
				addResourceButton.click();
				assertThat(addResourceButton).isNotEnabled();

				// add a duration
				eventFieldSelect.selectByVisibleText("duration");
				WebElement addDurationButton = $("#add-duration-button");
				assertThat(addDurationButton).isNotEnabled();
				$("#duration-hours-field").sendKeys("19");
				$("#duration-minutes-field").sendKeys("30");
				assertThat(addDurationButton).isEnabled();
				addDurationButton.click();
				assertThat(addDurationButton).isNotEnabled();

				// add a note
				eventFieldSelect.selectByVisibleText("note");
				WebElement addNoteButton = $("#add-note-button");
				assertThat(addNoteButton).isNotEnabled();
				$("#note-value-field").sendKeys("nice views");
				assertThat(addNoteButton).isEnabled();
				addNoteButton.click();
				assertThat(addNoteButton).isNotEnabled();

				// add a rating
				eventFieldSelect.selectByVisibleText("rating");
				WebElement addRatingButton = $("#add-rating-button");
				$("#rating-value-field").sendKeys("80");
				assertThat(addRatingButton).isEnabled();
				addRatingButton.click();

				saveEventButton.click();

				wait.withMessage("event count").until(ExpectedConditions.textToBePresentInElement(By.id("event-count"), "1"));
				WebElement row = driver.findElement(By.className("event-row"));
				WebElement action = driver.findElement(By.className("event-edit-action"));
				new Actions(driver).moveToElement(row).click(action).perform();

				wait.withMessage("edit event dialog is displayed").until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-event-dialog")));
				WebElement cancelEventButton = $("#cancel-event-button");
				cancelEventButton.click();


				// edit event: add another tag, delete a field

				// add benchpress and weather event

				// add events programmatically, refresh

				// import/export

				// add & configure widgets

				// test filtering & paging


				// sign up
				$("#sign-up-link").click();
				assertThat($("#sign-up-dialog")).isDisplayed();
				assertThat($("#sign-up-message")).isNotDisplayed();
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-username").sendKeys("jdoe");
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-password").sendKeys("password123");
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-password-repeat").sendKeys("password");
				assertThat($("#sign-up-button")).isNotEnabled();
				$("#sign-up-email").sendKeys("jdoe@zenobase.com");
				assertThat($("#sign-up-button")).isEnabled();
				$("#sign-up-button").click();
				assertThat($("#sign-up-dialog")).isDisplayed();
				assertThat($("#sign-up-message")).isDisplayed();
				$("#sign-up-password-repeat").sendKeys("123");
				$("#sign-up-button").click();

				wait.withMessage("view title equals user name").until(ExpectedConditions.textToBePresentInElement(By.id("user-title"), "jdoe"));
				assertThat(driver.findElements(By.className("bucket-link"))).as("bucket link").hasSize(1);
				assertThat($("#sign-up-banner")).isNotDisplayed();
				assertThat($("#alert-banner")).isNotDisplayed();
				assertThat($("#sign-in-link")).isNotDisplayed();
				assertThat($("#sign-out-link")).isDisplayed();
				assertThat($("#user-profile-link")).hasText("jdoe");

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
				assertThat($("#prev-buckets-button")).isNotEnabled();
				assertThat($("#next-buckets-button")).isNotEnabled();
				$("#refresh-buckets-link").click();

				// test paging
				wait.withMessage("next buckets button is enabled before paging").until(ExpectedConditions.elementToBeClickable(By.id("next-buckets-button")));
				assertThat($("#prev-buckets-button")).isNotEnabled();
				$("#next-buckets-button").click();

				wait.withMessage("prev buckets button is enabled after paging").until(ExpectedConditions.elementToBeClickable(By.id("prev-buckets-button")));
				assertThat($("#next-buckets-button")).isNotEnabled();
				$("#prev-buckets-button").click();


				// log back out

				// access as guest

				// log in

				// publish bucket

				// log out

				// access as guest

				// log back in


				// close account
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
		});
	}

	protected WebElement $(String selector) {
		return driver.findElement(By.cssSelector(selector));
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
		driver.quit();
	}
}
