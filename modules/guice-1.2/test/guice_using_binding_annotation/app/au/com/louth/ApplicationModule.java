package au.com.louth;

import au.com.louth.annotations.Loud;
import au.com.louth.annotations.Quiet;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Car.class).annotatedWith(Quiet.class).toInstance(new Toyota());
		bind(Car.class).annotatedWith(Loud.class).toInstance(new Ford());
		bind(Car.class).annotatedWith(Names.named("Foo")).toInstance(new Holden());
	}

}
