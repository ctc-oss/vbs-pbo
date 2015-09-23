package com.ctc.vht.pbos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSink;

/**
 * Models a single PBO header entry
 * @author wassj
 *
 */
@SuppressWarnings("unused" /* there are several unused fields in the spec */)
class Header {
	/**
	 * the filename \0 + 20; the 20 bytes are packaging, original size, reserved, timestamp, file size
	 */
	static final int emptyHeaderSize = 21;

	private final String filename;
	private final boolean compressed = false;
	private final int originalSize = 0;
	private final int timestamp = 0;
	private final int fileSize;

	// non-final; calculated after all headers are created
	private int fileOffset;


	/**
	 * create a {@link ByteProcessor} for reading header entries
	 */
	public static ByteProcessor<List<Header>> processor() {
		return new HeadersProcessor();
	}


	/**
	 * create a marking header (0 filename length)
	 */
	public static Header marker() {
		return new Header();
	}


	/**
	 * @param filename
	 * @param dataSize
	 */
	Header(final String filename, final int dataSize) {
		Preconditions.checkNotNull(filename);
		Preconditions.checkArgument(!filename.isEmpty(), "empty filename");
		this.filename = filename;
		this.fileSize = dataSize;
	}


	private Header() {
		this.filename = "";
		this.fileSize = 0;
	}


	/**
	 * the name this file should be written out to
	 * it may contain subdirectories
	 */
	public String getFilename() {
		return filename;
	}


	/**
	 * the size of the datablock in bytes
	 */
	public int getFileSize() {
		return fileSize;
	}


	/**
	 * get the starting point in the pbo for the file
	 */
	public int getFileOffset() {
		return fileOffset;
	}


	/**
	 * calculate the size of this header
	 */
	public int size() {
		return filename.length() + emptyHeaderSize;
	}


	/**
	 * write this header to the {@link ByteSink}
	 * @param byteSink
	 * @throws Exception
	 */
	public void write(final ByteSink byteSink)
		throws Exception {

		final ByteBuffer buffer = ByteBuffer.allocate(size()).order(ByteOrder.LITTLE_ENDIAN);
		for (final char ch : filename.toCharArray()) {
			buffer.put((byte)ch);
		}
		buffer.put((byte)0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(fileSize);
		byteSink.write(buffer.array());
	}


	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("header{ name:").append(filename).append(" size:").append(fileSize).append(" }");
		return buffer.toString();
	}


	/**
	 * process all headers returing a List of Header objects
	 * @author wassj
	 */
	static class HeadersProcessor
		implements ByteProcessor<List<Header>> {

		private final List<Header> headers = Lists.newArrayList();


		public List<Header> getResult() {
			return headers;
		}


		public boolean processBytes(final byte[] buf, final int off, final int len)
			throws IOException {

			final ByteBuffer buffer = ByteBuffer.wrap(buf, off, len).order(ByteOrder.LITTLE_ENDIAN);
			for (Optional<Header> header = read(buffer); header.isPresent(); header = read(buffer)) {
				headers.add(header.get());
			}

			// init with the size of all headers
			int currentDataOffset = sum(headers) + emptyHeaderSize;
			for (final Header header : headers) {
				header.fileOffset = currentDataOffset;
				currentDataOffset += header.getFileSize();
			}
			return false;
		}


		Optional<Header> read(final ByteBuffer buffer) {
			final StringBuilder filename = new StringBuilder();
			for (byte ch = buffer.get(); ch != 0; ch = buffer.get()) {
				filename.append((char)ch);
			}

			final int packaging = buffer.getInt();
			final int originalSize = buffer.getInt();
			final int reserved = buffer.getInt();
			final int timestamp = buffer.getInt();
			final int datasize = buffer.getInt();

			/*
			 * the end of the header section is marked with an empty length filename
			 */
			if (filename.length() > 0) {
				return Optional.of(new Header(filename.toString(), datasize));
			}
			return Optional.absent();
		}
	}


	static int sum(final Collection<Header> headers) {
		int sum = 0;
		for (final Header header : headers) {
			sum += header.size();
		}
		return sum;
	}
}
