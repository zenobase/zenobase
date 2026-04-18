package com.zenobase.json;

import java.io.IOException;

/**
 * Base class for streaming JSON responses. Subclasses implement onReady()
 * to write JSON content to the stream.
 */
public abstract class JsonChunks {

	protected abstract void onReady(JsonStream stream) throws IOException;
}
