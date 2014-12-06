package com.zenobase.testing;

import static org.mockito.Mockito.doAnswer;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import com.google.common.collect.ImmutableList;

import com.zenobase.common.Callback;

public class CallbackAnswer<T> implements Answer<Callback<T>> {

	private final ImmutableList<T> values;

	private CallbackAnswer(ImmutableList<T> values) {
		this.values = values;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Callback<T> answer(InvocationOnMock invocation) throws Throwable {
		for (Object arg : invocation.getArguments()) {
			if (arg instanceof Callback) {
				for (T value : values) {
					((Callback<T>) arg).call(value);
				}
				break;
			}
		}
		return null;
	}

	public static <T> Stubber doCallback(T value) {
		return doAnswer(new CallbackAnswer<>(ImmutableList.of(value)));
	}
}
