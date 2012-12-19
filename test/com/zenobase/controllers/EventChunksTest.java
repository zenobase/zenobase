package com.zenobase.controllers;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.json.JsonStream;
import com.zenobase.json.Nodes;
import com.zenobase.search.EventSearch;
import com.zenobase.services.EventRepository;

public class EventChunksTest {

	@Test
	public void test() throws IOException {

		final int total = 102;
		final String bucketId = Generator.id();
		final EventRepository events = mock(EventRepository.class);
		when(events.find(eq(bucketId), any(EventSearch.class)))
			.thenReturn(fakeResult(total, 100), fakeResult(total, 2));

		ObjectNode result = onReady(new EventChunks(events, bucketId, null));

		assertThat(result).path(EventListController.EVENTS.getName()).hasSize(total);
	}

	private static ObjectNode fakeResult(int total, int size) {
		ObjectNode fakeResult = Nodes.newObject();
		EventSearch.TOTAL.setValue(fakeResult, total);
		for (int i = 0; i < size; ++i) {
			ObjectNode fakeEvent = Nodes.newObject();
			fakeEvent.put("@id", Generator.id());
			EventListController.EVENTS.addValue(fakeResult, fakeEvent);
		}
		return fakeResult;
	}

	private static ObjectNode onReady(EventChunks chunks) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JsonStream stream = new JsonStream(out);
		chunks.onReady(stream);
		stream.close();
		return Nodes.readObject(out.toByteArray());
	}
}
