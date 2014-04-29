package com.zenobase.tasks.mapmyfitness;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class WorkoutsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WorkoutsResult result = new WorkoutsResult(readObject("WorkoutsResultTest.json"), TESTER, true);
		List<Workout> workouts = result.getWorkouts();
		assertThat(workouts).hasSize(3);
		Workout workout = workouts.get(2);
		assertThat(workout.getTypeId()).isEqualTo("9");
		assertThat(workout.getRouteId()).isEqualTo("400640020");
		Event event = new Event(workout.getEvent().getId());
		event.setValue(Event.TIMESTAMP, DateTime.parse("2014-04-24T22:36:33+00:00").withZone(DateTimeZone.forID("America/Los_Angeles")));
		event.setValue(Event.DURATION, Duration.standardSeconds(2992));
		event.setValue(Event.COUNT, 1000);
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf("3.34 mi"));
		event.setValue(Event.FREQUENCY, DecimalMeasure.<Frequency>valueOf("115 bpm"));
		event.setValue(Event.AUTHOR, TESTER);
		event.setValue(Event.SOURCE, new Resource("MapMyFitness", "http://www.mapmyfitness.com/workout/547836386"));
		assertThat(workout.getEvent()).isEqualTo(event);
	}
}
