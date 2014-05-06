package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** The File->Save As->Jpeg command (FileSaver.saveAsJpeg()) uses
      this plugin to save images in JPEG format when running Java 2. The
      path where the image is to be saved is passed to the run method. */
public class JpegWriter implements PlugIn {

	public static final int DEFAULT_QUALITY = 75;
	private static int quality;
	
    static {setQuality(ij.Prefs.getInt(ij.Prefs.JPEG, DEFAULT_QUALITY));}

    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null)
	 return;
        imp.startTiming();
        saveAsJpeg(imp,arg);
        IJ.showTime(imp, imp.getStartTime(), "JpegWriter: ");
    } 

    void saveAsJpeg(ImagePlus imp, String path) {
        //IJ.log("saveAsJpeg: "+path);
        int width  = imp.getWidth();
        int height = imp.getHeight();
        BufferedImage   bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
	    OutputStream f;
	    if (path.equals("-")) {
		f = System.out;
	    } else {
                f = new FileOutputStream(path);
	    }
            Graphics g = bi.createGraphics();
            g.drawImage(imp.getImage(), 0, 0, null);
            g.dispose();
            
            ImageWriter iw = ImageIO.getImageWritersByFormatName("jpeg").next();
	    ImageWriteParam param = iw.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality/100.0f);
            MemoryCacheImageOutputStream mc = new MemoryCacheImageOutputStream(f); 
            iw.setOutput(mc);
            IIOImage iio = new IIOImage(bi, null, null);
            iw.write(null, iio, param);
            iw.dispose();
            f.close();
        }
        catch (Exception e) {
            System.err.println("Caught exception:"+e);
           IJ.error("Jpeg Writer", ""+e);
        }
    }

	/** Specifies the image quality (0-100). 0 is poorest image quality,
		highest compression, and 100 is best image quality, lowest compression. */
    public static void setQuality(int jpegQuality) {
        quality = jpegQuality;
    	if (quality<0) quality = 0;
    	if (quality>100) quality = 100;
    }

    public static int getQuality() {
        return quality;
    }

}
