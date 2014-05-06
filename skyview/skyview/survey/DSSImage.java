package skyview.survey;


/** This class instantiates DSS images.
 *  These are read only images.
 */

import skyview.survey.Image;
import skyview.geometry.WCS;
import skyview.executive.Settings;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

/** This class defines an image as the combination
 *  of a set of pixel values and a WCS describing the
 *  pixel coordinates.
 */
public class DSSImage extends Image {
    
    // Define an array of 28x28 pointers to a double array.
    private int[][] dssData;
    private static char[] suffixes={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
				    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                                    'u', 'v', 'w', 'x', 'y', 'z'};
    
    private String directory;
    private String plate;
    
    private String[] urlPrefixes = Settings.getArray("LocalURL");
    private int nTiles   = 28;
    private int tileSize = 500;
    
    private int xSize;
    private int ySize;

    private int subimageCount = 0;
    private int[] dssIndex = new int[60];
    static  String lastImage;
    
    /** Construct a DSS image */
    public DSSImage (String directory) throws Exception {
	
	// DSS1 images are scanned as a 14000x13999 image and
	// broken up into 28x28 tiles of 500x500 pixels.
	// DSS2 images are scanned as a 23040x23040 image and
	// broken up into 30x30 tiles of 768x768 pixels.
	// The DSS header files do not seem to have any information
	// specifying the tiling, so that this is encoded
	// directly in the source code.
	
	this.directory   = directory;
	int slash = directory.lastIndexOf('/');
	
	plate     = directory.substring(slash+1);
	Header h  = getHeader(directory+"/"+plate+".hhh");
	WCS    w  = new WCS(h);
	int[]  axes = w.getHeaderNaxis();
	xSize = axes[0];
	ySize = axes[1];
	if (xSize > 14000) {
	    nTiles   = 30;
	    tileSize = 768;
	}
	
	initialize(null, w, xSize, ySize, 1);
	setName(plate);
    }
    
    /** Get a pixels data associated with the image. */
    public double  getData(int npix) {
	
	if (dssData == null) {
	    dssData = new int[nTiles*nTiles][];
	}
	
	int x = npix % xSize;
	int y = npix / xSize;
	
	int px = x/tileSize;
	int py = y/tileSize;
	
	
	x = x%tileSize;
	y = y%tileSize;
	
	int tile = px + nTiles*py;
	if (dssData[tile] == null) {
            String tileName = plate+"."+suffixes[py]+suffixes[px];
	    try {
		String file = directory+"/"+tileName;
		file = Util.replacePrefix(file, urlPrefixes);
		lastImage = file;
		
		if (file.startsWith("http:")) {
		    dssData[tile] = HDecompressor.decompress(new java.net.URL(file).openStream());
		} else {
	            dssData[tile] = HDecompressor.decompress(new java.io.FileInputStream(file));
		}
	    } catch (Exception e) {
                System.err.println("Tile decompression error for tile:"+tileName);
		e.printStackTrace(System.err);
                String warning = "";
                if (Settings.has("_warning")) {
                    warning = Settings.get("_warning")+"<br>";                    
                }
                Settings.put("_warning", warning+"HDecompress decompression error for tile:"+tileName);
                dssData[tile] = emptyTile(px, py);
	    }
	    
	    // Keep only the last few subimages in memory to avoid
	    // using it all up!
	    int idx = subimageCount%dssIndex.length;
	    if (subimageCount > dssIndex.length) {
		dssData[dssIndex[idx]] = null;
	    }
	    dssIndex[idx]  = tile;
	    subimageCount += 1;
	}
//	System.err.printf("Pixel: %d, px,py: %d %d; x,y: %d %d, val %d\n",
//			         npix, px, py, x, y, dssData[tile][x+tileSize*y]);
			  
	return (double) dssData[tile][x+tileSize*y];
    }
    
    /** Create an empty tile to replace one that cannot be read */
    int[] emptyTile(int px, int py) {
        int nx = tileSize;
        if (px*tileSize + nx > xSize) {
            nx = xSize - px*tileSize;
        }
        int ny = tileSize;
        if (py*tileSize + ny > ySize) {
            ny = ySize - py*tileSize;
        }
        return new int[nx*ny];        
    }
    
    /** Get the data as an array */
    public double[] getDataArray() {
	throw new Error("getDataArray: Invalid operation on DSS image");
    }
    
    /** Set the Data associated with the image.
     */
    public void setData(int npix, double newData) {
	throw new Error("setData: Invalid operation on DSS image");
    }
    
    /** Clear the data array */
    public void clearData() {
	dssData = null;
    }
    
    /** Set the data array */
    public void setDataArray(double[] newData) {
	throw new Error("setDataArray: Invalid operation on DSS image");
    }
    
    private Header getHeader(String headerFile) throws Exception {
	
	byte[] buf = new byte[80];
	Header h   = new Header();
	java.io.DataInputStream in;
	
	headerFile = Util.replacePrefix(headerFile, urlPrefixes);
	if (headerFile.startsWith("http:")) {
	    in = new java.io.DataInputStream(
		   new java.io.BufferedInputStream(
		     new java.net.URL(headerFile).openStream() ) );
	} else {
	    in = new java.io.DataInputStream(
	           new java.io.BufferedInputStream(
	             new java.io.FileInputStream(headerFile) ) );
	}
	nom.tam.util.Cursor c = h.iterator();
        while (true) {
	    in.readFully(buf);
	    
	    String s = new String(buf);
	    HeaderCard hc = new HeaderCard(s);
	    String key = hc.getKey();
	    if (!key.equals("") && !key.equals("COMMENT") && !key.equals("HISTORY")) {
	        c.add(hc.getKey(), hc);
	    }
	    if (s.substring(0, 4).equals("END ")) {
		break;
	    }
	}
	// Putting  an offset of 1 matches the results with the
	// old SkyView pages.  This gets some support when we
	// download cutouts from the ST image server which seems
	// to have an offset also.
	// 
	// Note that this is only for the old DSS headers.
	// The new ones have CNPIX1/2 = 0
	
	if (h.getDoubleValue("CNPIX1") == 1  &&
	    h.getDoubleValue("CNPIX2") == 1) {
	    h.addValue("CNPIX1", 0, null);
	    h.addValue("CNPIX2", 0, null);
	}
	return h;
    }
    
    /** Is this image tiled */
    public boolean isTiled() {
	return true;
    }
}
	
