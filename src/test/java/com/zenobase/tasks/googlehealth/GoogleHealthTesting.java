package com.zenobase.tasks.googlehealth;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Semi-automated tests that exercise each Google Health task against the live API. Run with:
 *
 * <pre>
 * -Doauth.apiKey=...  -Doauth.apiSecret=...
 * -Doauth.token=...   -Doauth.refresh=...
 * </pre>
 *
 * If {@code oauth.token} is not supplied, the test prints an authorization URL and waits on STDIN for the full callback
 * URL (including the {@code code} query parameter) so it can exchange it. See {@link TaskTestingSupport} for details.
 * All but the first test are {@link Disabled} by default to keep the default local test run cheap — enable the ones
 * you're iterating on.
 */
public class GoogleHealthTesting extends TaskTestingSupport {

	private static final String TIMEZONE = "America/Los_Angeles";
	private static final String MARKER = "2026-01-01T00:00:00Z";

	@Test
	public void testSteps() {
		runInApplication(
			new GoogleHealthStepsTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "steps").put("hourly", false)
		);
	}

	@Test
	@Disabled
	public void testCardio() {
		runInApplication(
			new GoogleHealthCardioTaskManager(newCredentialsManager()),
			Nodes.newObject()
				.put("marker", MARKER)
				.put("timezone", TIMEZONE)
				.put("tag", "heart rate")
				.put("hourly", false)
		);
	}

	@Test
	@Disabled
	public void testSleep() {
		runInApplication(
			new GoogleHealthSleepTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "sleep")
		);
	}

	@Test
	@Disabled
	public void testWeight() {
		runInApplication(
			new GoogleHealthWeightTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "weight").put("metric", true)
		);
	}

	@Test
	@Disabled
	public void testActivities() {
		runInApplication(
			new GoogleHealthActivitiesTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("metric", true)
		);
	}

	@Test
	@Disabled
	public void testFood() {
		runInApplication(
			new GoogleHealthFoodTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "food")
		);
	}

	@Test
	@Disabled
	public void testBurn() {
		runInApplication(
			new GoogleHealthBurnTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "burn").put("hourly", false)
		);
	}

	@Test
	@Disabled
	public void testHrv() {
		runInApplication(
			new GoogleHealthHrvTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "hrv")
		);
	}

	@Test
	@Disabled
	public void testSpo2() {
		runInApplication(
			new GoogleHealthSpo2TaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "spo2")
		);
	}

	@Test
	@Disabled
	public void testRespiratory() {
		runInApplication(
			new GoogleHealthRespiratoryTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", MARKER).put("timezone", TIMEZONE).put("tag", "respiration")
		);
	}

	@Test
	@Disabled
	public void testTemperature() {
		runInApplication(
			new GoogleHealthTemperatureTaskManager(newCredentialsManager()),
			Nodes.newObject()
				.put("marker", MARKER)
				.put("timezone", TIMEZONE)
				.put("tag", "temperature")
				.put("metric", true)
		);
	}

	@Override
	protected GoogleHealthCredentialsManager newCredentialsManager() {
		return new GoogleHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
