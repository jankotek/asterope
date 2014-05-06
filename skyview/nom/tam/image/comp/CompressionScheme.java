/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nom.tam.image.comp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

/**
 *
 * @author tmcglynn
 */
public interface CompressionScheme {
    
    /** Return the 'name' of the compression scheme */
    public abstract String name();
    
    /** Initialize the compression scheme with any appropriate
     *  parameters.
     */
    public abstract void initialize(Map<String, String> params);
    
    /** Return a stream which compresses the input. */
    public abstract byte[] compress(byte[] in) throws IOException;
    
    /** Return a stream with decompresses the input. */
    public abstract byte[] decompress(byte[] in, int length) throws IOException;
    
    /** Update the FITS header and compression parameterswith
     * information about the compression 
     */
    public abstract void updateForWrite(Header hdr , Map<String,String> parameters)
        throws FitsException;
    
    /** Get the parameters indicated by a given header */
    public abstract Map<String,String> getParameters(Header hdr);
    
}
