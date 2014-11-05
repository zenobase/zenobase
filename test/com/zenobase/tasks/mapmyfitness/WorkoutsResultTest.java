package com.zenobase.tasks.mapmyfitness;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Pace;
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
		event.setValue(Event.TIMESTAMP, dateTime("2014-04-24T15:36:33-07:00"));
		event.setValue(Event.DURATION, Duration.standardSeconds(2992));
		event.setValue(Event.COUNT, 1000);
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf("3.34 mi"));
		event.setValue(Event.VELOCITY,  DecimalMeasure.<Velocity>valueOf("4.02 mph"));
		event.setValue(Event.PACE,  DecimalMeasure.<Pace>valueOf("895 s/mi"));
		event.setValue(Event.FREQUENCY, DecimalMeasure.<Frequency>valueOf("115 bpm"));
		event.setValue(Event.AUTHOR, TESTER);
		event.setValue(Event.SOURCE, new Resource("MapMyFitness", "http://www.mapmyfitness.com/workout/547836386"));
		assertThat(workout.getEvent()).isEqualTo(event);
	}
}
