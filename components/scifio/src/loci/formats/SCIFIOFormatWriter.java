package loci.formats;

import java.awt.image.ColorModel;
import java.io.IOException;

import net.imglib2.meta.Axes;

import loci.formats.codec.CodecOptions;
import loci.formats.meta.MetadataRetrieve;
import loci.legacy.context.LegacyContext;
import ome.scifio.ByteArrayPlane;
import ome.scifio.DefaultMetadataOptions;
import ome.scifio.Format;
import ome.scifio.Metadata;
import ome.scifio.Plane;
import ome.scifio.Writer;
import ome.scifio.io.RandomAccessInputStream;
import ome.xml.meta.OMEMetadata;

/**
 * Abstract superclass of all biological file format writers that have been
 * converted to SCIFIO components. Defers to an ome.scifio.Writer 
 * 
 * @see ome.scifio.Writer
 * 
 * @author Mark Hiner
 *
 * @deprecated see ome.scifio.Writer
 */
@Deprecated
public abstract class SCIFIOFormatWriter extends FormatWriter {

  // -- Fields --

  /** Scifio Writer for deference */
  protected Writer writer;
  
  /** Scifio Format corresponding to this writer */
  protected Format format;
  
  //TODO could make this a Plane and then cache either a ByteArrayPlane or BufferedImagePlane as needed
  /**
   * Cached ByteArrayPlane for reusing byte arrays with this writer
   */
  private ByteArrayPlane bPlane = null;

  /** */
  protected Metadata meta;

  // -- Constructor --

  public SCIFIOFormatWriter(String format, String suffix) {
    super(format, suffix);
  }

  public SCIFIOFormatWriter(String format, String[] suffixes) {
    super(format, suffixes);
  }

  // -- IFormatWriter API methods --

  /* @see IFormatWriter#changeOutputFile(String) */
  @Override
  public void changeOutputFile(String id) throws FormatException, IOException {
    try {
      writer.changeOutputFile(id);
    }
    catch (ome.scifio.FormatException e) {
      throw new FormatException(e);
    }
  }

  /* @see IFormatWriter#saveBytes(int, byte[]) */
  @Override
  public void saveBytes(int no, byte[] buf) throws FormatException, IOException
  {
    try {
      writer.savePlane(getSeries(), no, planeCheck(buf, 0, 0,
          writer.getMetadata().getAxisLength(getSeries(), Axes.X),
          writer.getMetadata().getAxisLength(getSeries(), Axes.Y)));
    }
    catch (ome.scifio.FormatException e) {
      throw new FormatException(e);
    }
  }

  /* @see IFormatWriter#savePlane(int, Object) */
  @Override
  public void savePlane(int no, Object plane)
    throws FormatException, IOException
  {
    // NB: Writers use byte arrays by default as the native type.
    if (!(plane instanceof byte[])) {
      throw new IllegalArgumentException("Object to save must be a byte[]");
    }
    try {
      writer.savePlane(getSeries(), no, planeCheck((byte[])plane, 0, 0,
          writer.getMetadata().getAxisLength(getSeries(), Axes.X),
          writer.getMetadata().getAxisLength(getSeries(), Axes.Y)));
    }
    catch (ome.scifio.FormatException e) {
      throw new FormatException(e);
    }
  }

  /* @see IFormatWriter#savePlane(int, Object, int, int, int, int) */
  @Override
  public void savePlane(int no, Object plane, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    // NB: Writers use byte arrays by default as the native type.
    if (!(plane instanceof byte[])) {
      throw new IllegalArgumentException("Object to save must be a byte[]");
    }
    try {
      writer.savePlane(getSeries(), no, planeCheck((byte[])plane, x, y, w, h), x, y, w, h);
    }
    catch (ome.scifio.FormatException e) {
      throw new FormatException(e);
    }
  }

  /* @see IFormatWriter#setSeries(int) */
  @Override
  public void setSeries(int series) throws FormatException {
    if (series < 0) throw new FormatException("Series must be > 0.");
    if (series >= writer.getMetadata().getImageCount()) {
      throw new FormatException("Series is '" + series +
        "' but MetadataRetrieve only defines " +
        writer.getMetadata().getImageCount() + " series.");
    }
    this.series = series;
  }

  /* @see IFormatWriter#getSeries() */
  @Override
  public int getSeries() {
    return series;
  }

  /* @see IFormatWriter#setInterleaved(boolean) */
  @Override
  public void setInterleaved(boolean interleaved) {
    writer.getMetadata().setInterleaved(getSeries(), interleaved);
  }

  /* @see IFormatWriter#isInterleaved() */
  @Override
  public boolean isInterleaved() {
    return writer.getMetadata().isInterleaved(getSeries());
  }

  /* @see IFormatWriter#setValidBitsPerPixel(int) */
  @Override
  public void setValidBitsPerPixel(int bits) {
    writer.getMetadata().setBitsPerPixel(getSeries(), bits);
  }

  /* @see IFormatWriter#canDoStacks() */
  @Override
  public boolean canDoStacks() {
    return false;
  }

  /* @see IFormatWriter#setMetadataRetrieve(MetadataRetrieve) */
  @Override
  public void setMetadataRetrieve(MetadataRetrieve retrieve) {
     metadataRetrieve = retrieve;
  }

  /* @see IFormatWriter#getMetadataRetrieve() */
  @Override
  public MetadataRetrieve getMetadataRetrieve() {
    return metadataRetrieve;
  }

  /* @see IFormatWriter#setColorModel(ColorModel) */
  @Override
  public void setColorModel(ColorModel model) {
    writer.setColorModel(model);
  }

  /* @see IFormatWriter#getColorModel() */
  @Override
  public ColorModel getColorModel() {
    return writer.getColorModel();
  }

  /* @see IFormatWriter#setFramesPerSecond(int) */
  @Override
  public void setFramesPerSecond(int rate) {
    writer.setFramesPerSecond(rate);
  }

  /* @see IFormatWriter#getFramesPerSecond() */
  @Override
  public int getFramesPerSecond() {
    return writer.getFramesPerSecond();
  }

  /* @see IFormatWriter#getCompressionTypes() */
  @Override
  public String[] getCompressionTypes() {
    return writer.getCompressionTypes();
  }

  /* @see IFormatWriter#setCompression(compress) */
  @Override
  public void setCompression(String compress) throws FormatException {
    // check that this is a valid type
    try {
      writer.setCompression(compress);
    }
    catch (ome.scifio.FormatException e) {
      throw new FormatException(e);
    }
  }

  /* @see IFormatWriter#setCodecOptions(CodecOptions) */
  @Override
  public void setCodecOptions(CodecOptions options) {
    writer.setCodecOptions(options);
  }

  /* @see IFormatWriter#getCompression() */
  @Override
  public String getCompression() {
    return writer.getCompression();
  }

  /* @see IFormatWriter#getPixelTypes() */
  @Override
  public int[] getPixelTypes() {
    return writer.getPixelTypes(getCompression());
  }

  /* @see IFormatWriter#getPixelTypes(String) */
  @Override
  public int[] getPixelTypes(String codec) {
    return writer.getPixelTypes(codec);
  }

  /* @see IFormatWriter#isSupportedType(int) */
  @Override
  public boolean isSupportedType(int type) {
    return writer.isSupportedType(type);
  }

  /* @see IFormatWriter#setWriteSequentially(boolean) */
  @Override
  public void setWriteSequentially(boolean sequential) {
    writer.setWriteSequentially(sequential);
  }

  // -- Deprecated IFormatWriter API methods --

  // TODO decide to remove or not
//  /**
//   * @deprecated
//   * @Override
//   * @see IFormatWriter#saveBytes(byte[], boolean)
//   */
//  public void saveBytes(byte[] bytes, boolean last)
//    throws FormatException, IOException
//  {
//    try {
//      writer.savePlane(planeCheck(bytes, 0, 0,
//          writer.getMetadata().getAxisLength(getSeries(), Axes.X),
//          writer.getMetadata().getAxisLength(getSeries(), Axes.Y)));
//    }
//    catch (ome.scifio.FormatException e) {
//      throw new FormatException(e);
//    }
//  }
//
//  /**
//   * @deprecated
//   * @Override
//   * @see IFormatWriter#saveBytes(byte[], int, boolean, boolean)
//   */
//  public void saveBytes(byte[] bytes, int series, boolean lastInSeries,
//    boolean last) throws FormatException, IOException
//  {
//    try {
//      writer.savePlane(bytes, series, lastInSeries, last);
//    }
//    catch (ome.scifio.FormatException e) {
//      throw new FormatException(e);
//    }
//  }
//
//  /**
//   * @deprecated
//   * @Override
//   * @see IFormatWriter#savePlane(Object, boolean)
//   */
//  public void savePlane(Object plane, boolean last)
//    throws FormatException, IOException
//  {
//    try {
//      writer.savePlane(plane, 0, last, last);
//    }
//    catch (ome.scifio.FormatException e) {
//      throw new FormatException(e);
//    }
//  }
//
//  /**
//   * @deprecated
//   * @Override
//   * @see IFormatWriter#savePlane(Object, int, boolean, boolean)
//   */
//  public void savePlane(Object plane, int series, boolean lastInSeries,
//    boolean last) throws FormatException, IOException
//  {
//    // NB: Writers use byte arrays by default as the native type.
//    if (!(plane instanceof byte[])) {
//      throw new IllegalArgumentException("Object to save must be a byte[]");
//    }
//    try {
//      writer.savePlane(planeCheck((byte[])plane), series, lastInSeries, last);
//    }
//    catch (ome.scifio.FormatException e) {
//      throw new FormatException(e);
//    }
//  }

  // -- IFormatHandler API methods --

  /* @see IFormatHandler#setId(String) */
  @Override
  public void setId(String id) throws FormatException, IOException {
    if (id.equals(currentId)) return;
    writer.close();
    currentId = id;
    try {
      Metadata meta = writer.getFormat().createMetadata();
      MetadataRetrieve retrieve = getMetadataRetrieve();
      
      if (!ome.xml.meta.OMEXMLMetadata.class.
          isAssignableFrom(retrieve.getClass()))
      {
        throw new FormatException("MetadataRetrieve was not an " +
        		"instance ome.xml.meta.OMEXMLMetadata. Instead, got: " +
            retrieve.getClass());
      }
      OMEMetadata omeMeta = new OMEMetadata(LegacyContext.get(),
          (ome.xml.meta.OMEXMLMetadata)retrieve);
      
      omeMeta.setMetadataOptions(new DefaultMetadataOptions());
      
      // convert the metadata retrieve to ome.scifio.Metadata
      RandomAccessInputStream stream = new RandomAccessInputStream(LegacyContext.get(), id);
      omeMeta.setSource(stream);
      writer.scifio().translators().translate(omeMeta, meta);
      stream.close();

      meta.setDatasetName(id);
      
      writer.setMetadata(meta);
      writer.setDest(id);
    }
    catch (ome.scifio.FormatException e) {
      throw new FormatException(e);
    }
  }

  /* @see IFormatHandler#close() */
  @Override
  public void close() throws IOException {
    writer.close();
  }
  
  // -- Helper Methods --
  
  // TODO need this in Reader too.. need to abstract this, and maybe expand the signature options
  
  protected Plane planeCheck(byte[] buf, int x, int y, int w, int h) {
    if (bPlane == null)
      bPlane = new ByteArrayPlane(writer.getContext());
    
    bPlane.populate(writer.getMetadata().get(getSeries()), buf, x, y, w, h);
    return bPlane;
  }
}
