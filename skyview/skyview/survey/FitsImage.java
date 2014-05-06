package skyview.survey;

import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataInputStream;

import skyview.geometry.TransformationException;
import skyview.survey.Image;
import skyview.survey.ImageFactory;

import skyview.executive.Settings;

/** This class defines an image gotten by reading a file */

public class FitsImage extends Image {
    
    private String fitsFile;
    private Header fitsHeader;
    
    private float[] smallData;
    
    public String getFilename() {
        return fitsFile;
    }
    
    public void clearData() {
        smallData = null;
        super.clearData();
    }
    
    public FitsImage(String file) throws SurveyException {
	
	Header h;
	skyview.geometry.WCS wcs;
	
	setName(file);
	data = null;
	
	this.fitsFile = file;
	
        nom.tam.util.ArrayDataInput inp = null;
	try {
	    Fits f = new Fits(file);
	    inp = f.getStream();
	
	    h = new Header(inp);
	    
	    //  Kludge to accommodate DSS2
	    if (h.getStringValue("REGION") != null) {
		setName(h.getStringValue("REGION")+":"+file);
	    }
	    
	} catch (Exception e) {
	    throw new SurveyException("Unable to read file:"+fitsFile);
	} finally {
	    if (inp != null) {
		try {
		    inp.close();
		} catch (Exception e) {
		}
	    }
	}
		   
	
        int naxis = h.getIntValue("NAXIS");
	if (naxis < 2) {
	    throw new SurveyException("Invalid FITS file: "+fitsFile+".  Dimensionality < 2");
	}
	int nx = h.getIntValue("NAXIS1");
	int ny = h.getIntValue("NAXIS2");
	int nz = 1;
	
	if (h.getIntValue("NAXIS") > 2) {
	    nz = h.getIntValue("NAXIS3");
	}
	
	if (naxis > 3) {
	    for(int i=4; i <= naxis; i += 1) {
		if (h.getIntValue("NAXIS"+i) > 1) {
		    throw new SurveyException("Invalid FITS file:"+fitsFile+".  Dimensionality > 3");
		}
	    }
	}
	
	try {
	    if (Settings.has("PixelOffset")) {
		String[] crpOff= Settings.getArray("PixelOffset");
		try {
		    double d1 = Double.parseDouble(crpOff[0]);
		    double d2 = d1;
		    if (crpOff.length > 0) {
			d1 = Double.parseDouble(crpOff[1]);
		    }
		    h.addValue("CRPIX1", h.getDoubleValue("CRPIX1")+d1, "");
		    h.addValue("CRPIX2", h.getDoubleValue("CRPIX2")+d2, "");
		} catch (Exception e) {
		    System.err.println("Error adding Pixel offset:"+Settings.get("PixelOffset"));
		    // Just go on after letting the user know.
		}
	    }
	    wcs = new skyview.geometry.WCS(h);
	} catch (TransformationException e) {
	    throw new SurveyException("Unable to create WCS for file:"+fitsFile+" ("+e+")");
	}
	
        
        
	try {
	    initialize(null, wcs, nx, ny, nz);
	} catch(TransformationException e) {
	    throw new SurveyException("Error generating tranformation for file: "+file);
	}
	fitsHeader = h;
    }
    
    private boolean usingFloat = false;
    
    
    /** Defer reading the data until it is asked for. */
    public double getData(int npix) {
	
	Fits     f = null;
	BasicHDU hdu;
        Object xdata;
        
	
	if (data == null && smallData == null) {
            double bzero = 0;
            double bscale = 1;
	    
	    try {
		// We're going to read everything, so
		// don't worry if it's a file or not.
		
		try {
		    java.net.URL url = new java.net.URL(fitsFile);
		    f = new Fits(url);
		
		} catch (Exception e) {
		    // Try it as a file
		}
		    
		if (f == null) {
                    f   = new Fits(Util.getResourceOrFile(fitsFile));
		}
                ArrayDataInput inp = f.getStream();
                // Read the header.
                Header hdr = Header.readHeader(inp);
                bzero = hdr.getDoubleValue("BZERO", 0);
                bscale = hdr.getDoubleValue("BSCALE", 1);
                // Now positioned to read data, but we 
                // want to read it raw...
                int bitpix = hdr.getIntValue("BITPIX");
                int dim = hdr.getIntValue("NAXIS");
                int len = 1;
                for (int i=1; i<(dim+1); i += 1) {
                    len *= hdr.getIntValue("NAXIS"+i);
                }
                
                Class cls;
                switch (bitpix) {
                    case 8: cls = byte.class;
                                break;
                    case 16: cls = short.class;
                                break;    
                    case 32: cls = int.class;
                                break;
                    case 64: cls = long.class;
                                break;
                    case -32: cls = float.class;
                                break;
                    case -64: cls = double.class;
                                break;
                    default: throw new FitsException("Invalid BITPIX value"+bitpix);
                }
                xdata = ArrayFuncs.newInstance(cls, len);
                inp.readLArray(xdata);
                inp.close();
	    } catch(Exception e) {
		throw new Error("Error reading FITS data for file: "+fitsFile+"\n\nException was:"+e);
	    }
	    
	    // Data may not be double (and it may be scaled)
	    if (!(xdata instanceof float[])) {
                
                boolean bytearray = xdata instanceof byte[];
                
		xdata = nom.tam.util.ArrayFuncs.convertArray(xdata, double.class, true);
		data = (double[]) xdata;
                // Bytes are signed integers in Java, but unsigned
	        // in FITS, so if we are reading in a byte array
	        // we'll need to convert the negative values.
            
                if (bytearray || bscale != 1 || bzero != 0) {

                    for (int i = 0; i < data.length; i += 1) {
                        if (bytearray && data[i] < 0) {
                            data[i] += 256;
                        }
                        data[i] = bscale * data[i] + bzero;
                    }
                }
            } else {
                smallData = (float[]) xdata;
                float fscale = (float) bscale;
                float fzero  = (float) bzero;
                if (fscale != 1 || fzero != 0) {
                    for (int i=0; i<smallData.length; i += 1) {
                        smallData[i] = fscale*smallData[i]+fzero;
                    }
                }
                usingFloat = true;
            }
        }
        double val;
        if (!usingFloat) {
            val = data[npix];
        } else {
            val =  (double) smallData[npix];
        }
        return val;
    }
    
    public double[] getDataArray() {
        if (data == null && smallData != null) {
            data = (double[]) ArrayFuncs.convertArray(smallData, double.class);
            return data;
        } else {
            return super.getDataArray();
        }
    }
    
    public Header getHeader() {
	return fitsHeader;
    }		// Bytes are signed integers in Java, but unsigned
		// in FITS, so if we are reading in a byte array
		// we'll need to convert the negative values.

}
