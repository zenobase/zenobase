package com.zenobase.json;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Base class for streaming JSON responses. Subclasses implement onReady()
 * to write JSON content to the stream.
 */
public abstract class JsonChunks {

	public void writeTo(OutputStream out) {
		try {
			JsonStream stream = new JsonStream(out);
			onReady(stream);
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract void onReady(JsonStream stream) throws IOException;
}
