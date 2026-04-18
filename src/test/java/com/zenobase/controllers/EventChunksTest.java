package com.zenobase.controllers;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.json.JsonStream;
import com.zenobase.json.Nodes;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.Search;

public class EventChunksTest {

	@Test
	public void test() throws IOException {
		int total = 102;
		String bucketId = Generator.id();
		EventRepository events = mock(EventRepository.class);

		doAnswer(
			(Answer<Void>) invocation -> {
				Callback<ObjectNode> callback = (Callback<ObjectNode>) invocation.getArgument(2);
				for (int i = 0; i < total; ++i) {
					ObjectNode fakeEvent = Nodes.newObject();
					fakeEvent.put("@id", Generator.id());
					callback.call(fakeEvent);
				}
				return null;
			}
		)
			.when(events)
			.find(eq(bucketId), any(Search.class), any(Callback.class));

		ObjectNode result = onReady(new EventChunks(events, bucketId, List.of()));

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
