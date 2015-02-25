package com.zenobase.controllers;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.json.JsonStream;
import com.zenobase.json.Nodes;
import com.zenobase.search.Search;
import com.zenobase.services.EventRepository;

public class EventChunksTest {

	@Test
	public void test() throws IOException {

		final int total = 102;
		final String bucketId = Generator.id();
		final EventRepository events = mock(EventRepository.class);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Callback<ObjectNode> callback = (Callback<ObjectNode>) invocation.getArgumentAt(2, Callback.class);
				for (int i = 0; i < total; ++i) {
					ObjectNode fakeEvent = Nodes.newObject();
					fakeEvent.put("@id", Generator.id());
					callback.call(fakeEvent);
				}
				return null;
			}
		}).when(events).find(eq(bucketId), any(Search.class), any(Callback.class));

		ObjectNode result = onReady(new EventChunks(events, bucketId, ImmutableList.<String>of()));

		assertThat(result).path(EventListController.EVENTS.getName()).hasSize(total);
	}

	private static ObjectNode onReady(EventChunks chunks) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JsonStream stream = new JsonStream(out);
		chunks.onReady(stream);
		stream.close();
		return Nodes.readObject(out.toByteArray());
	}
}
