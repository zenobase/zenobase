import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class MultibinderTest {

	private Injector injector;

	@Before
	public void setUp() {
		injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Foo.class).in(Singleton.class);
				Multibinder<Processor> processors = Multibinder.newSetBinder(binder(), Processor.class);
				processors.addBinding().to(IncrementProcessor.class);
				processors.addBinding().to(RecursiveProcessor.class);
			}
		});
	}

	@Test
	public void test() {
		Foo foo = injector.getInstance(Foo.class);
		Assert.assertEquals(3, foo.compute());
	}

	interface Processor {
		int process(int value);
	}

	static class IncrementProcessor implements Processor {

		@Override
		public int process(int value) {
			return value + 1;
		}
	}

	static class RecursiveProcessor implements Processor {

		private final Set<Processor> processors;

		@Inject
		public RecursiveProcessor(Set<Processor> processors) {
			this.processors = processors;
		}

		@Override
		public int process(int value) {
			return value + processors.size();
		}
	}

	static class Foo {

		private final Set<Processor> processors;

		@Inject
		public Foo(Set<Processor> processors) {
			this.processors = processors;
		}

		public int compute() {
			int result = 0;
			for (Processor processor : processors) {
				result = processor.process(result);
			}
			return result;
		}
	}
}
