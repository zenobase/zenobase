package controllers;

import play.Logger;
import play.mvc.Result;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;

public class Timed extends Simple {

	@Override
	public Result call(Context context) throws Throwable {
		long t0 = System.nanoTime();
		Result result = delegate.call(context);
		long t1 = System.nanoTime();
		Logger.info("Time: " + (t1 - t0) / (1000 * 1000));
		return result;
	}
}
