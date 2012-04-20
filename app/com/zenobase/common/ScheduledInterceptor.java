package com.zenobase.common;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import play.Logger;
import play.libs.Akka;
import akka.util.Duration;

public class ScheduledInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Logger.info("Scheduling method invocation...");
		Akka.system().scheduler().scheduleOnce(Duration.Zero(), new Runnable() {
			@Override
			public void run() {
				try {
					invocation.proceed();
				} catch (Throwable e) {
					Logger.error("Scheduled method invocation failed", e);
				}
			}
		});
		return null;
	}
}
