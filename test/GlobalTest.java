import static play.test.Helpers.*;

import org.junit.Test;

public class GlobalTest {

	@Test
	public void test() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {

			}
		});
	}

}
