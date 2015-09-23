package com.ctc.vht.pbos;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author wassj
 *
 */
public class HeaderSizesTest {

	@Test
	public void headerSize() {
		final String filename = UUID.randomUUID().toString();
		final Header header = new Header(filename, 0);
		final int expected = Header.emptyHeaderSize + filename.length();
		Assert.assertEquals(header.size(), expected);
	}


	@Test
	public void markerHeaderSize() {
		final Header header = Header.marker();
		final int expected = Header.emptyHeaderSize;
		Assert.assertEquals(header.size(), expected);
	}


	@Test(expectedExceptions = IllegalArgumentException.class)
	public void guardEmptyFilename() {
		new Header("", 0);
		Assert.fail();
	}


	@Test(expectedExceptions = NullPointerException.class)
	public void guardNullFilename() {
		new Header(null, 0);
		Assert.fail();
	}
}
