/*-
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2017 SCIFIO developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package io.scif.formats;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.Field;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
import io.scif.util.FormatTools;

import java.io.IOException;

import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;

import org.scijava.io.handle.DataHandle;
import org.scijava.io.handle.DataHandle.ByteOrder;
import org.scijava.io.location.Location;
import org.scijava.plugin.Plugin;

/**
 * The file format reader for Kontron IMG files
 * <p>
 * Kontron IMG files are generated by a Kontron controller computer attached to
 * Alan Boyde's Zeiss DSM 962 scanning electron microscope. They are 8-bit
 * greyscale images with a 128-byte header, which contains the image width and
 * height and an identifier sequence.
 * </p>
 *
 * @author Richard Domander (Royal Veterinary College, London)
 * @author Michael Doube (Royal Veterinary College, London)
 */
@Plugin(type = Format.class, name = "Kontron")
public class KontronFormat extends AbstractFormat {

	/** Kontron IMGs start with this sequence */
	public static final byte[] KONTRON_ID = { 0x1, 0x0, 0x47, 0x12, 0x6D,
		(byte) 0xB0 };
	/** Length of the header in a Kontron file */
	public static final int HEADER_BYTES = 128;

	@Override
	protected String[] makeSuffixArray() {
		return new String[] { "img" };
	}

	public static class Metadata extends AbstractMetadata {

		@Field(label = "Width from file header")
		private long width;
		@Field(label = "Height from file header")
		private long height;

		public long getWidth() {
			return width;
		}

		public long getHeight() {
			return height;
		}

		public void setHeight(final long height) {
			this.height = height;
		}

		public void setWidth(final long width) {
			this.width = width;
		}

		@Override
		public void populateImageMetadata() {
			// Missing format metadata from IJ1: grey-scale, whiteIsZero = false
			createImageMetadata(1);
			final ImageMetadata metadata = get(0);
			// little endian a.k.a. intel byte order
			metadata.setLittleEndian(true);
			metadata.setBitsPerPixel(8);
			metadata.setPixelType(FormatTools.UINT8);
			metadata.setOrderCertain(true);
			metadata.setPlanarAxisCount(2);
			metadata.setAxes(new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(
				Axes.Y));
			metadata.setAxisLengths(new long[] { width, height });
		}
	}

	public static class Parser extends AbstractParser<Metadata> {

		@Override
		public void typedParse(final DataHandle<Location> stream,
			final Metadata meta, final SCIFIOConfig config) throws IOException,
			FormatException
		{
			stream.setOrder(ByteOrder.LITTLE_ENDIAN);
			stream.seek(KONTRON_ID.length);
			final short width = stream.readShort();
			final short height = stream.readShort();
			meta.setWidth(width);
			meta.setHeight(height);
		}
	}

	public static class Checker extends AbstractChecker {

		@Override
		public boolean suffixSufficient() {
			return false;
		}

		@Override
		public boolean suffixNecessary() {
			return false;
		}

		@Override
		public boolean isFormat(final DataHandle<Location> stream)
			throws IOException
		{
			final byte[] fileStart = new byte[KONTRON_ID.length];
			final int read = stream.read(fileStart);
			if (read != KONTRON_ID.length) {
				return false;
			}

			for (int i = 0; i < KONTRON_ID.length; i++) {
				if (fileStart[i] != KONTRON_ID[i]) {
					return false;
				}
			}

			return true;
		}
	}

	public static class Reader extends ByteArrayReader<Metadata> {

		@Override
		protected String[] createDomainArray() {
			return new String[] { FormatTools.SEM_DOMAIN };
		}

		@Override
		public ByteArrayPlane openPlane(final int imageIndex, final long planeIndex,
			final ByteArrayPlane plane, final long[] planeMin, final long[] planeMax,
			final SCIFIOConfig config) throws FormatException, IOException
		{
			final DataHandle<Location> stream = getHandle();
			stream.seek(HEADER_BYTES);
			return readPlane(stream, imageIndex, planeMin, planeMax, plane);
		}
	}
}
