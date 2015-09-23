package com.ctc.vht.pbos;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

/**
 * entry point to the PBO API
 * @author wassj
 *
 */
public class Pbos {

	/**
	 * return an input stream for the specified file within the pbo
	 * @param pbo
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static InputStream streamFileFrom(final Path pbo, final String filename)
		throws Exception {

		Preconditions.checkNotNull(pbo);
		Preconditions.checkNotNull(filename);
		Preconditions.checkArgument(!filename.isEmpty(), "empty filename");

		final File file = pbo.toFile();
		Preconditions.checkArgument(file.exists());

		for (final Header header : Files.asByteSource(file).read(Header.processor())) {
			if (filename.equals(header.getFilename())) {
				try (final InputStream is = Files.asByteSource(file).openBufferedStream()) {
					final ByteBuffer buffer = ByteBuffer.allocate(header.getFileSize());
					is.skip(header.getFileOffset());
					is.read(buffer.array(), 0, header.getFileSize());
					return new ByteArrayInputStream(buffer.array());
				}
			}
		}
		throw new FileNotFoundException(String.format("file[%s] not found in pbo[%s]", filename, pbo));
	}


	/**
	 * unpack the pbo at the specified path to the specified path
	 * @param source input pbo
	 * @param target output directory
	 * @throws Exception
	 */
	public static void unpack(final Path source, final Path target)
		throws Exception {

		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(target);

		final File sourceFile = source.toFile();
		Preconditions.checkArgument(sourceFile.exists(), "source[%s] doesnt exist", sourceFile);

		final File targetDirectory = target.toFile();
		Preconditions.checkArgument(!targetDirectory.exists() || targetDirectory.isDirectory(), "target is not a directory; %s", targetDirectory);
		Preconditions.checkArgument(!targetDirectory.exists() || targetDirectory.listFiles().length == 0, "non-empty target directory; %s", targetDirectory);
		Preconditions.checkState(targetDirectory.mkdirs(), "could not create directory; %s", targetDirectory);

		for (final Header header : Files.asByteSource(sourceFile).read(Header.processor())) {
			try (final InputStream is = Files.asByteSource(sourceFile).openBufferedStream()) {
				final ByteBuffer buffer = ByteBuffer.allocate(header.getFileSize());
				is.skip(header.getFileOffset());
				is.read(buffer.array(), 0, header.getFileSize());
				write(buffer, header.getFilename(), targetDirectory);
			}
		}
	}


	public static void pack(final Path source, final Path target)
		throws Exception {

		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(target);

		final File sourceDirectory = source.toFile();
		Preconditions.checkArgument(sourceDirectory.exists(), "source[%s] doesnt exist", sourceDirectory);
		Preconditions.checkArgument(sourceDirectory.isDirectory(), "source[%s] is not a directory", sourceDirectory);

		final File targetFile = target.toFile();
		Preconditions.checkArgument(!targetFile.exists(), "target file already exists; %s", targetFile);
		Preconditions.checkState(targetFile.getParentFile().exists() || targetFile.getParentFile().mkdirs(), "couldnt create target path; %s", targetFile.getParentFile());
		Preconditions.checkState(targetFile.createNewFile(), "could not create file; %s", targetFile);

		final List<File> files = Lists.newArrayList(sourceDirectory.listFiles());
		Preconditions.checkArgument(!files.isEmpty(), "directory empty; %s", sourceDirectory);
		Collections.sort(files);

		final ByteSink byteSink = Files.asByteSink(targetFile, FileWriteMode.APPEND);
		for (final File file : files) {
			final ByteSource byteSource = Files.asByteSource(file);
			new Header(file.getName(), (int)byteSource.size()).write(byteSink);
		}
		new Header("", 0).write(byteSink);
		for (final File file : files) {
			byteSink.write(Files.asByteSource(file).read());
		}
		byteSink.write(ByteBuffer.allocate(21) //
			.order(ByteOrder.LITTLE_ENDIAN)
			.put((byte)0)
			.put(Files.asByteSource(targetFile).hash(Hashing.sha1()).asBytes())
			.array());
	}


	static void write(final ByteBuffer buffer, final String filename, final File root)
		throws Exception {

		final File unpacked = new File(root, filename);
		if (!unpacked.getParentFile().equals(root)) {
			unpacked.getParentFile().mkdirs();
		}
		unpacked.createNewFile();
		Files.asByteSink(unpacked).write(buffer.array());
	}
}
