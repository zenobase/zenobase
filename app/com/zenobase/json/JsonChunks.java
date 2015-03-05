package com.zenobase.json;

import java.io.IOException;

import play.Logger;
import play.libs.F;
import play.mvc.Results;

import com.zenobase.common.Generator;
import com.zenobase.io.ChunksOutputStream;

public abstract class JsonChunks extends Results.ByteChunks {

	private final String id = Generator.id();

	@Override
	public void onReady(Results.Chunks.Out<byte[]> out) {
		try {
			Logger.info("Connected stream {}", id);
			out.onDisconnected(new F.Callback0() {
				@Override
				public void invoke() throws Throwable {
					Logger.info("Disconnected stream {}", id);
				}
			});
			JsonStream stream = new JsonStream(new ChunksOutputStream(out));
			onReady(stream);
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			out.close();
			Logger.info("Closed stream {}", id);
		}
	}

	protected abstract void onReady(JsonStream stream) throws IOException;
}
