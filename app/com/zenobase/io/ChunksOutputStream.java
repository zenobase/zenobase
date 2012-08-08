package com.zenobase.io;

import java.io.OutputStream;
import java.util.Arrays;

import play.mvc.Results.Chunks.Out;

public class ChunksOutputStream extends OutputStream {

	private final Out<byte[]> out;

	public ChunksOutputStream(Out<byte[]> out) {
		this.out = out;
	}

	@Override
	public void write(int b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(byte[] b) {
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		write(Arrays.copyOfRange(b, off, off + len));
	}
}
