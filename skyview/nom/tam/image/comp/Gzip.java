

package nom.tam.image.comp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

/**
 * 
 * @author tmcglynn
 */
public class Gzip implements CompressionScheme {

    public String name() {
        return "GZIP_1";
    }

    public void initialize(Map<String, String> params) {
        // No initialization.
    }

    /**
     * Compress data.  If non-byte data is to be compressed
     * it should be converted to a byte array first (e.g.,
     * by writing it to a ByteArrayOutputStream).
     * @param in       The input data to be compressed.
     * @return         The compressed array.
     * @throws IOException 
     */
    public byte[] compress(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream zout = new GZIPOutputStream(out);
        zout.write(in);
        zout.close();
        return out.toByteArray();
    }

    /**
     * Decompress data.  If non-byte data is to be compressed
     * it should be read from the resulting array (e.g.,
     * by reading it from a ByteArrayInputStream).
     * @param  in       The compressed array.
     * @param length   The number of output elements expected.
     *                 For GZIP encoding this is ignored. 
     * @return         The decompressed array.
     * @throws IOException 
     */
    public byte[] decompress(byte[] in, int length) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream  sin  = new ByteArrayInputStream(in);
        GZIPInputStream zin  = new GZIPInputStream(sin);
        byte[] buffer = new byte[1024];
        int len;
        while ( (len=zin.read(buffer)) > 0) {
            out.write(buffer,0, len);
        }
        out.close();
        return out.toByteArray();
    }
    
    public void updateForWrite(Header hdr, Map<String,String> parameters) 
     throws FitsException {
    }
    
    public Map<String,String> getParameters(Header hdr) {
        return new HashMap<String,String>();
    }
}
