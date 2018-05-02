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
		assertThat(workouts).hasSize(2);

		Workout w1 = workouts.get(0);
		assertThat(w1.getTypeId()).isEqualTo("9");
		assertThat(w1.getRouteId()).isEqualTo("400640020");
		Event e1 = new Event(w1.getEvent().getId());
		e1.setValue(Event.TIMESTAMP, dateTime("2014-04-24T15:36:33-07:00"));
		e1.setValue(Event.DURATION, Duration.standardSeconds(2992));
		e1.setValue(Event.COUNT, 1000);
		e1.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf("3.3 mi"));
		e1.setValue(Event.VELOCITY,  DecimalMeasure.<Velocity>valueOf("4.0 mph"));
		e1.setValue(Event.PACE,  DecimalMeasure.<Pace>valueOf("895 s/mi"));
		e1.setValue(Event.FREQUENCY, DecimalMeasure.<Frequency>valueOf("115 bpm"));
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, new Resource("MapMyFitness", "http://www.mapmyfitness.com/workout/547836386"));
		assertThat(w1.getEvent()).isEqualTo(e1);

		Workout w2 = workouts.get(1);
		assertThat(w2.getTypeId()).isEqualTo("285");
		assertThat(w2.getRouteId()).isNull();
		Event e2 = new Event(w2.getEvent().getId());
		e2.setValue(Event.TIMESTAMP, dateTime("2014-11-06T11:00:00-08:00"));
		e2.setValue(Event.DURATION, Duration.standardMinutes(10));
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, new Resource("MapMyFitness", "http://www.mapmyfitness.com/workout/787788009"));
		assertThat(w2.getEvent()).isEqualTo(e2);
	}
}
