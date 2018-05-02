package com.zenobase.json;

import java.io.IOException;

import play.mvc.Results;

import com.zenobase.io.ChunksOutputStream;

public abstract class JsonChunks extends Results.ByteChunks {

	@Override
	public void onReady(Results.Chunks.Out<byte[]> out) {
		try {
			JsonStream stream = new JsonStream(new ChunksOutputStream(out));
			onReady(stream);
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			out.close();
		}
	}

	protected abstract void onReady(JsonStream stream) throws IOException;
}
