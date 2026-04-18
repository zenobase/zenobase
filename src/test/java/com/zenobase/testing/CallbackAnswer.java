package com.zenobase.testing;

import static org.mockito.Mockito.doAnswer;

import com.zenobase.common.Callback;
import java.util.List;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

public class CallbackAnswer<T> implements Answer<Callback<T>> {

	private final List<T> values;

	private CallbackAnswer(List<T> values) {
		this.values = values;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Callback<T> answer(InvocationOnMock invocation) {
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
		return doAnswer(new CallbackAnswer<>(List.of(value)));
	}
}
