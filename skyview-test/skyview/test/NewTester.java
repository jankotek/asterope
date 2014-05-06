package skyview.test;

import java.awt.image.BufferedImage;
import skyview.executive.Imager;
import skyview.executive.Settings;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static skyview.test.Util.regress;
import junit.framework.JUnit4TestAdapter;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;

import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import nom.tam.fits.Fits;
import nom.tam.util.ArrayFuncs;

/**
 * This class does basic testing of the features of the SkyView JAR. It
 * primarily does end-to-end tests of SkyView requests and does a regression of
 * the results against previous values. The results of the previous tests are
 * loaded using a Settings file, comparison.settings. <p> Using the standard Ant
 * script all testing is done in a temp subdirectory. The comparison.settings
 * and inputtest1.fits file are copied from the source/skyview/test directory
 * into this directory. The actual tests are run using JUnit. JUnit invokes all
 * methods with the Test annotation. If an assertion in any method fails it
 * continues with the next test. <p> This program does not do many unit tests of
 * individual functions, nor does it test for bugs that may come from particular
 * combinations of features selected by the user. Additional tests can easily be
 * added as appropriate however. <p> The Ant build script that compiles that
 * Java code will run this script with
 * <code>ant -f build/skyview.xml test</code>. To run only some tests the user
 * can set the environment variables MIN_TEST and MAX_TEST and only the tests
 * that are between these values will be run. Tests are organized in groups with
 * each test numbered. The following ranges define the category associated with
 * each test. 
 * <dl> 
 *   <dt>1.x <dd> Checking individual surveys. 
 *   <dt>2.x <dd> Samplers and Mosaickers 
 *   <dt>3.x <dd> Projections 
 *   <dt>4.x <dd> Coordinate Systems 
 *   <dt>5.x <dd> Rotation and Translation 
 *   <dt>6.x <dd> Pixel scale
 *   <dt>7.x <dd> Image sizes in pixels 
 *   <dt>8.x <dd> Settings 
 *   <dt>9.x <dd> Different ways of specifying positions 
 *   <dt>10.x <dd> Quicklook image formats
 *   <dt>11.x <dd> Gridding 
 *   <dt>12.x <dd> Contouring 
 *   <dt>13.x <dd> Smoothing
 *   <dt>14.x <dd> Catalogs 
 *   <dt>15.x <dd> Look up tables 
 *   <dt>16.x <dd> RGB images
 *   <dt>17.x <dd> Image intensity scaling (log, linear, sqrt) 
 *   <dt>18.x <dd> Plot parameters 
 * </dl> 
 * Additional tests can be added to any of these, and new
 * categories of tests can be added. Most of the tests &lt; 10.x test the data
 * in the resulting FITS image. Most of the higher tests test the data in the
 * quicklook image. 
 * <p> Changes to SkyView code will inevitably create changes
 * in the reference values even when there is no error, e.g., just the text in a
 * graphic overlay. Tests will fail when the comparison.settings file has a
 * value for the sum of the appropriate file, and the generated file does not
 * match. To update these, simply delete the corresponding entry in
 * comparison.settings. If a regression test is run and there is no entry in the
 * comparison.settings file, a line is written to the upd.settings file. After
 * the test is run, this file can be concatenated to the comparison.settings (in
 * the source area) for future regression testing.
 */
public class NewTester {

    private static FileWriter os;
    private static boolean    first = true;
    
    /** min/maxTest may be overriden by the logical variables MIN_TEST and MAX_TEST */
    private static String minTest = "0";
    private static String maxTest = "999999";
    
    /** The designator for the test.  New tests should be added at the end of existing tests
     *  or within a test but immediately befor an 'base.up()'. 
     */
    private static HCounter base;
    
    
    /** Test files that may need to be extracted from the JAR file */
    private static String[] testFiles = {
        "inputtest1.fits", "inputtest2.fits", "inputtest3.fits",
        "plot1.drw", "plot2.drw", "plot3.drw",
        "comparison.settings"
    };
    
    // Case insensitive map (only functions used are overriden)
    private static class UMap {
        public HashMap<String,String> base = new HashMap<String,String>();
        void put(String key, String val) {
            base.put(key.toLowerCase(), val);
        }
        void remove(String key) {
            base.remove(key.toLowerCase());
        }
        String get(String key) {
            return base.get(key.toLowerCase());
        }
        Set<String> keySet() {
            return base.keySet();
        }
        void clear() {
            base.clear();
        }
    }
    
    private static UMap currentSettings = new UMap();
    private static Map<String, String> oldResults = new HashMap<String, String>();

    public static void main(String args[]) {
        if (System.getenv("BZIP_DECOMPRESSOR") == null) {
            System.err.println("Please Specify a BZIP2 Decompressor in the BZIP_DECOMPRESSOR logical name");
            System.exit(1);
        }
        org.junit.runner.JUnitCore.main("skyview.test.NewTester");
    }

    @BeforeClass
    public static void initialize() {

        // Did the user choose to limit the tests.
        if (System.getenv("MIN_TEST") != null) {
            minTest = System.getenv("MIN_TEST");
        }
        if (System.getenv("MAX_TEST") != null) {
            maxTest = System.getenv("MAX_TEST");
        }
        
        // Read in files needed for tests.
        for (String file: testFiles) {
            System.err.println("Extracting file:"+file);
            try {
                fileExtract(file);
            } catch (IOException e) {
                System.err.println("Initialization: Unable to extract file:"+file);
            }            
        }

        // Get any old information about tests.
        if (new File("comparison.settings").exists()) {
            Settings.updateFromFile("comparison.settings");
        }
        for (String key : Settings.getKeys()) {
            oldResults.put(key, Settings.get(key));
        }

        // Set up a file giving information about results.  Append to
        // anything in the file.
        try {
            os = new FileWriter("upd.settings", true);
        } catch (Exception e) {
            System.err.println("Unable to initialize upd.settings");
        }
    }

    @Test
    public void testSurveys() throws Exception {

        base = new HCounter(1);

        currentSettings.clear();
        String[] surveys = new String[]{
            "dss", "dss1r", "dss1b", "dss2r", "dss2b", "dss2ir",
            "pspc2int", "pspc1int", "pspc2cnt", "pspc2exp",
            "2massh", "2massj", "2massk",
            "sdssi", "sdssu", "sdssz", "sdssg", "sdssr",
            "iras100", "iras60", "iras25", "iras12",
            "first", "nvss", "hriint",
            "rass-cnt broad", "rass-cnt hard", "rass-cnt soft",
            "halpha", "sfddust", "sfd100m",
            "shassa_c", "shassa_cc", "shassa_h", "shassa_sm",
            "4850mhz", "iris12", "iris25", "iris60", "iris100",
            "wfcf1", "wfcf2", "euve83", "euve171", "euve405", "euve555",
            "neat", "rass-int broad", "rass-int hard", "rass-int soft",
            "wisew1", "wisew2", "wisew3", "wisew4",
            "planck-030", "planck-044", "planck-070", "planck-100",
            "planck-143", "planck-217", "planck-353", "planck-545", "planck-857"
        };

        // Mellinger survey is done twice so that
        // we get full resolution and lower resolution images.
        String[] allSky = new String[]{
            "1420mhz", "408mhz", "heao1a",
            "cobeaam", "cobezsma",
            "comptel",
            "egretsoft", "egrethard", "egret3d",
            "rxte3_20K_sig", "rxte3_8k_sig", "rxte8_20k_sig",
            "nh", "0035mhz",
            "wmapk", "wmapka", "wmapq", "wmapv", "wmapw", "wmapilc",
            "mell-r", "mell-g", "mell-b",
            "intgal1735f", "intgal1760f", "intgal3580f", "intgal1735e", "intgal1735s"
        };

        String[] gc = new String[]{
            "co", "granat_sigma_sig", "granat_sigma_flux",
            "integralspi_gc",
            "rassbck1", "rassbck2", "rassbck3",
            "rassbck4", "rassbck5", "rassbck6", "rassbck7",
            "mell-r", "mell-g", "mell-b"
        };
        
        String[] goodsSurveys = new String[] {
            "GOODSNVLA", "GOODSMIPS", "GOODSACISFB", "GOODSACISSB","GOODSACISHB",
            "GOODS ACS B", "GOODS ACS V", "GOODS ACS I", "GOODS ACS Z",
            "GOODS IRAC 1", "GOODS IRAC 2", "GOODS IRAC 3", "GOODS IRAC 4"
        };
        
        String[] southGoodsSurveys = new String[] {
            "GOODS: VLT VIMOS U","GOODS: VLT VIMOS R",
            "GOODS: VLT ISAAC J", "GOODS: VLT ISAAC H", "GOODS: VLT ISAAC Ks",
            "HUDF: VLT ISAAC Ks", "CDFS: LESS"
        };


        // This set of surveys
        base.down();

        // Element within list.
        base.down();

        currentSettings.put("Position", "187.27791499999998,2.052388");


        for (int i = 0; i < surveys.length; i += 1) {
            currentSettings.put("Survey", surveys[i]);
            test(true);
        }
        base.up();
        base.down();

        // Try some all sky surveys
        currentSettings.put("Position", "0.,0.");
        currentSettings.put("Coordinates", "galactic");
        currentSettings.put("Projection", "Car");
        currentSettings.put("Pixels", "600,300");
        currentSettings.put("Size", "360,180");

        for (int i = 0; i < allSky.length; i += 1) {
            currentSettings.put("Survey", allSky[i]);
            test(true);
        }

        currentSettings.put("Size", "5");
        currentSettings.put("Pixels", "300");

        base.up();
        base.down();
        for (int i = 0; i < gc.length; i += 1) {
            currentSettings.put("Survey", gc[i]);
            test(true);
        }

        currentSettings.put("Position", "0., 90.");
        currentSettings.put("Coordinates", "ICRS");
        currentSettings.put("Pixels", "500,500");
        currentSettings.put("Projection", "Tan");

        base.up();
        base.down();

        currentSettings.put("Survey", "wenss");

        test(true);

        currentSettings.put("Position", "0., -90.");
        currentSettings.put("Survey", "sumss");
        test(true);

        currentSettings.put("Position", "10.,10.");
        // No coverage near 3c273
        currentSettings.put("Size", ".4");
        currentSettings.put("Survey", "galexnear");
        test(true);
        currentSettings.put("Survey", "galexfar");
        test(true);
        base.up();
        base.down();

        currentSettings.put("Size", "40");
        for (int i = 1; i < 6; i += 1) {
            currentSettings.put("Survey", "fermi" + i);
            test(true);
        }

        base.up();
        base.down();
        currentSettings.put("Position", "184.6,2.08");
        currentSettings.put("Size", "0.05");
        for (char c : new char[]{'Y', 'J', 'H', 'K'}) {
            currentSettings.put("Survey", "UKIDSS-" + c);
            test(true);
        }
        
        base.up();
        base.down();
        currentSettings.put("Position", "0.,0.");
        currentSettings.put("Coordinates", "Galactic");
        currentSettings.put("Size", "10");
        for (String type: new String[]{"snr", "flux"}) {
            for (int i=1; i<9; i += 1) {
                currentSettings.put("survey", "bat-"+type+"-"+i);
                test(true);
            }
        }
        currentSettings.put("survey", "bat-snr-sum");
        test(true);
        base.up();
        base.down();
        
        currentSettings.remove("Coordinates");
        
        currentSettings.put("survey","Stripe82VLA");
        currentSettings.put("Position", "346.,-0.2");
        currentSettings.put("Size", ".1");
        test(true);
        
        base.up();
        base.down();
        // Use GOODS north region
        currentSettings.put("Position","189.228621,62.238661");
        currentSettings.put("Size", "0.1");
        currentSettings.put("Pixels", "400");
        for (String survey: goodsSurveys) {
            currentSettings.put("survey", survey);
            test(true);
        }
        
        base.up();
        base.down();
        // Check scaling for ACS
        currentSettings.put("survey", "GOODS ACS B");
        currentSettings.put("Pixels", "400");
        
        currentSettings.put("Size", ".001");
        test(true);
        currentSettings.put("Size", ".01");
        test(true);
        currentSettings.put("Size", ".1");
        test(true);
        currentSettings.put("Size", "1");
        test(true);
        
        base.up();
        base.down();
        currentSettings.put("Size", "0.01");
        currentSettings.put("Position", "53.16222,-27.78932"); // CDFS       
        for (String survey: southGoodsSurveys) {
            currentSettings.put("survey", survey);
            test(true);
        }
    }

    public int jpegSum(String jpegName) throws Exception {

        InputStream is = null;
        try {
            is = new FileInputStream(jpegName);

            int sum = 0;
            BufferedImage img = ImageIO.read(is);

            Raster r = img.getData();
            int nx = r.getWidth();
            int ny = r.getHeight();
            int[] pix = new int[3];
            for (int i = 0; i < nx; i += 1) {
                for (int j = 0; j < ny; j += 1) {
                    pix = r.getPixel(i, j, pix);
                    sum += pix[0] + pix[1] + pix[2];
                }
            }
 
            is.close();
            return sum;
        } catch (Exception e) {
            if (is != null) {
                is.close();
            }
            
            // Can't handle this with ImageIO.  Just try regular IO and
            // sum the ints in the file.  We use int's rather than bytes
            // since we only have 256 different bytes.
            System.err.println("Unable to interpret quicklook file "+jpegName+" using ImageIO.");
            System.err.println("Summing file directly.");
            int sum = 0;
            is = new BufferedInputStream(new FileInputStream(jpegName));
            // Read sequences of 4 bytes to create an int.  If we are at EOF
            // then zero out extras.
            byte[] buf = new byte[4];
            int need = 4;
            int have = 0;
            int count = 0;
            try {
                while (true) {
                    buf[0] = buf[1] = buf[2] = buf[3] = 0;
                    need = 4;
                    have = 0;
                    
                    while (need > 0) {
                        int got = is.read(buf, have, need);
                        if (got <= 0) {
                            break;
                        }
                        count += got;
                        need -= got;
                        have += got;
                    }
                    // Got at least 1 byte.
                    if (have > 0) {
                        // Treat as big endian.
                        int val = ((buf[0]&0xFF) << 24) | ((buf[1]&0xFF) << 16) |
                                  ((buf[2]&0xFF) << 8) | (buf[3]&0xFF);
                        sum += val;
                    }
                    
                    // If we didn't get all the bytes, then an I/O returned 0 which
                    // means EOF.
                    if (need != 0) {
                        break;
                    }
                } 
            } catch (EOFException f) {
                // If we got an explicit EOF, then we may have read some bytes
                // already.  Add them in if so.  Any unread bytes are 0.
                if (have > 0) {
                    int val = ((buf[0]&0xFF) << 24) | ((buf[1]&0xFF) << 16) |
                              ((buf[2]&0xFF) << 8)  |  (buf[3]&0xFF);
                    sum += val;
                }
            }
            System.err.println(" Size of file is:"+count);
            return sum;
        }
    }

    public double fitsSum(String file) {
        double sum = 0;
        int n = 0;
        try {
            Fits f = new Fits(file);
            double[][] val = (double[][]) f.readHDU().getKernel();
            for (int i = 0; i < val.length; i += 1) {
                for (int j = 0; j < val[i].length; j += 1) {
                    n += 1;
                    if (!Double.isNaN(val[i][j])) {
                        sum += val[i][j];
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception reading file:" + file + " " + e);
        }
        if (n == 0) {
            return Double.NaN;
        } else {
            return sum;
        }
    }

    void execute() throws IOException, InterruptedException {
        String cmd = "";
        String prefix = base.toString();
        currentSettings.put("Output", prefix+".fits");
        List<String> args = new ArrayList<String>();

        args.add("java");
        args.add("-Djava.awt.headless=true");
        args.add("-jar");
        args.add("../../jar/skyview.jar");

        System.err.print("\nTest "+base.toString()+"\n   ");
        for (String key : currentSettings.keySet()) {
            String val = key + "=" + currentSettings.get(key);
            args.add(val);
            System.err.print(val+" ");
        }
        
        System.err.println("\n   Start at "+ new java.util.Date());
        ProcessBuilder pd = new ProcessBuilder(args);
        Process proc = pd.start();
        proc.waitFor();
        System.err.println("   Finish at "+new java.util.Date());
    }
    
    double[] getData() {
        String prefix = base.toString();
        try {
            execute();
            Fits f = new Fits(prefix + ".fits");
            double[][] data = (double[][]) f.readHDU().getKernel();
            return (double[]) ArrayFuncs.flatten(data);
        } catch (Exception e) {
            error(e);
            return null;
        }
    }

    boolean test(boolean fits) {
        boolean inc = false;
        try {
            if (included(base)) {
                inc = true;
                double val = 0;
                String prefix = base.toString();
                execute();
                if (fits) {
                    val = fitsSum(prefix + ".fits");
                } else {
                    if (new File(prefix+".jpg").exists()) {
                       val = jpegSum(prefix + ".jpg");
                    } else {
                        boolean found = false;
                        String[] suffixes = {
                           ".jpg", "_rgb.jpg", ".gif", ".bmp", ".tiff", ".jpeg", ".png"};
                        for (String suffix: suffixes) {                            
                            if (new File(prefix+suffix).exists()) {
                                found = true;
                                val = jpegSum(prefix +suffix);
                                break;                               
                            }
                        }
                        if (!found) {
                            throw new Exception("Unable to find quicklook file for "+prefix);
                        }
                    }
                }

                if (oldResults.containsKey(prefix)) {
                    double oval = Double.parseDouble(oldResults.get(prefix));
                    assertEquals(prefix, val, oval, 0);
                } else {
                    String b = base.toString();
                    os.write(printSettings("# "+b+": ")+"\n");
                    os.write(base.toString()+"="+val+"\n");
                    os.flush();
                }
            }
        } catch (Exception e) {
            error(e);
        } finally {
            base.increment();
        }
        return inc;
    }
    
    static void fileExtract(String name) throws IOException {
        if (new File(name).exists()) {
            System.err.println(name+" already exists, not extracted.");
            return;
        }
        
        System.err.println("Looking for file "+name+" in classpath in package skyview/test.");
        InputStream is = skyview.survey.Util.getResourceOrFile("skyview/test/"+name);
        OutputStream os = new FileOutputStream(name);
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);            
        }
        os.close();
        is.close();        
    }
    
    String printSettings(String prefix) {
        String out = "";
        if (prefix != null) {
            out = prefix;
        }
        for (String key: currentSettings.keySet()) {
            out += "  "+key+"="+currentSettings.get(key);
        }
        return out;
    }

    void error(Exception e) {

        System.err.println("Test: " + base.toString());
        System.err.println("   Exception:"+e);
        System.err.println(printSettings("   Settings:"));
    }

    @Test
    public void testSamplers() throws Exception {

        base = new HCounter(2);
        base.down();
        base.down();

        String[] samplers = new String[]{"NN", "LI", "Lanczos", "Lanczos3", "Lanczos4",
            "Spline", "Spline3", "Spline4", "Spline5", "Clip"};

        currentSettings.clear();
        currentSettings.put("Survey", "user");
        currentSettings.put("Size", "1.1");
        currentSettings.put("Pixels", "22");
        currentSettings.put("Userfile", "inputtest1.fits");
        currentSettings.put("Position", "0.,0.");
        currentSettings.put("Coordinates", "J2000");

        doTests(samplers, "Sampler", true);

        base.up();
        base.down();


        currentSettings.put("Sampler", "Clip");
        currentSettings.put("Survey", "IRIS100");
        currentSettings.put("Size", "30");
        currentSettings.put("Pixels", "500");
        doTests(new String[]{"skyview.process.IDMosaic"}, "mosaicker", true);

        currentSettings.put("Survey", "User");
        currentSettings.put("Userfile", "inputtest1.fits,inputtest2.fits,inputtest3.fits");
        currentSettings.put("Position", "0.,0.");
        currentSettings.put("Size", "10.1");
        currentSettings.put("Pixels", "101");
        currentSettings.put("Sampler", "Clip");
        currentSettings.put("Projection", "Car");
        String[] specialFinders = new String[]{"Bypass", "Overlap"};
        currentSettings.put("Mosaicker", "skyview.process.AddingMosaicker");

        doTests(specialFinders, "imagefinder", true);

    }

    /**
     * Should the current test be run?
     */
    private static boolean included(HCounter base) {
        return !base.before(minTest) && !base.after(maxTest);
    }

    double doTests(String[] options, String param, boolean fits) throws Exception {

        double grand = 0;
        for (int i = 0; i < options.length; i += 1) {
            if (included(base)) {
                currentSettings.put(param, options[i]);
                test(fits);
            }
        }
        currentSettings.remove(param);
        return grand;
    }

    private long fileSum(String filename) {
        long sum = 0;
        try {
            FileInputStream bf = new FileInputStream(filename);
            int byt;
            while ((byt = bf.read()) >= 0) {
                sum += byt;
            }
            bf.close();
        } catch (Exception e) {
        }
        return sum;
    }

    @Test
    public void testProjections() throws Exception {

        currentSettings.clear();

        String[] projections = {"Tan", "Sin", "Car", "Ait", "Csc", "Zea", "Arc", "Stg", "Sfl", "Hpx", "Tea"};

        currentSettings.put("Survey", "heao1a");
        currentSettings.put("Coordinates", "Galactic");
        currentSettings.put("Size", "90");
        currentSettings.put("Pixels", "300");
        currentSettings.put("Position", "0.,0.");

        base = new HCounter(3);
        base.down();
        base.down();
        doTests(projections, "Projection", true);

        base.up();
        base.down();
        String[] proj2 = {"Toa"};


        // Want to center Toa around the pole.
        //
        currentSettings.put("Coordinates", "J2000");
        currentSettings.put("Position", "0.,90.");
        currentSettings.put("Survey", "HEAO1A");
        currentSettings.put("Pixels", "200");
        currentSettings.put("Size", "90");
        currentSettings.put("Sampler", "Clip");
        currentSettings.put("ClipIntensive", "1");

        doTests(proj2, "projection", true);
        base.up();
        base.down();

        // Now do a tiled TOAST projection
        currentSettings.remove("Size");
        currentSettings.remove("Pixels");
        currentSettings.remove("Position");
        currentSettings.put("Level", "9");
        currentSettings.put("TileX", "49");
        currentSettings.put("TileY", "33");
        currentSettings.put("Subdir", "9");
        currentSettings.put("Survey", "DSS");
        doTests(proj2, "projection", true);

        base.up();
        base.down();
        currentSettings.remove("Level");
        currentSettings.remove("TileX");
        currentSettings.remove("TileY");
        currentSettings.remove("Subdir");
        
        currentSettings.put("Position", "0.,90.");
        currentSettings.put("survey", "HEAO1A");
        String[] proj3 = {"Toa", "Tea", "Tot"};
        doTests(proj3, "projection", true);
        base.up();
        base.down();
        String[] proj4 = {"Cah"};
        currentSettings.put("Position", "0,45");
        currentSettings.put("Size", "600,400");
        currentSettings.put("Pixels", "1200,800");
        doTests(proj4, "projection", true);
    }

    @Test
    public void testCoordinates() throws Exception {

        String[] coordinates = {"J2000", "B1950", "E2000", "H2000", "Galactic", "ICRS"};

        currentSettings.clear();
        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "90");
        currentSettings.put("pixels", "300");
        currentSettings.put("position", "0.,0.");


        base = new HCounter(4);
        base.down();
        doTests(coordinates, "coordinates", true);
    }

    @Test
    public void testRotation() throws Exception {

        String[] angles = {"0", "30", "60", "90", "180", "-30", "-90"};

        currentSettings.clear();
        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "90");
        currentSettings.put("coordinates", "galactic");
        currentSettings.put("pixels", "300");
        currentSettings.put("Sampler", "NN");
        currentSettings.put("position", "0.,0.");

        base = new HCounter(5);
        base.down();
        base.down();
        doTests(angles, "rotation", true);
        base.up();
        base.down();
        currentSettings.remove("rotation");

        currentSettings.put("survey", "dss");
        currentSettings.put("projection", "Tan");
        currentSettings.put("coordinates", "J2000");
        currentSettings.remove("scale");
        currentSettings.remove("size");
        currentSettings.put("position", "187.27791499999998,2.052388");

        doTests(new String[]{"1"}, "min", true);

        currentSettings.put("pixels", "150");

        base.up();
        base.down();

        // RefCoords test
        currentSettings.remove("offset");
        currentSettings.put("pixels", "500,250");
        currentSettings.put("size", "380,180");
        currentSettings.put("projection", "Ait");
        currentSettings.put("Position", "0.,90.");
        currentSettings.put("Survey", "408Mhz");
        doTests(new String[]{"0.,1.", "0.,90.", "0.,-89.", "179.,0."}, "RefCoords", true);
    }

    @Test
    public void testScale() throws Exception {

        String[] scales = {"0.25", "0.25,0.25", "0.5,0.25", "0.25,0.5", "0.1"};

        currentSettings.clear();
        currentSettings.put("Survey", "heao1a");
        currentSettings.put("Position", "0.,0.");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "90");
        currentSettings.put("coordinates", "Galactic");
        currentSettings.put("pixels", "300");
        currentSettings.put("Sampler", "NN");

        base = new HCounter(6);
        base.down();

        doTests(scales, "scale", true);
    }

    @Test
    public void testPixel() throws Exception {
        String[] pixels = {"300", "300,150", "150,300", "10,10"};
        currentSettings.clear();
        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "90");
        currentSettings.put("coordinates", "galactic");
        currentSettings.put("pixels", "300");
        currentSettings.put("Sampler", "NN");
        currentSettings.put("position", "0.,0.");

        base = new HCounter(7);
        base.down();
        doTests(pixels, "pixels", true);
    }

    @Test
    public void testSettings() {

        base = new HCounter(8);
        if (included(base)) {
            System.err.println("Testing settings");
            assertTrue("XXX not set", !Settings.has("xxx"));
            Settings.add("XXX", "aaa");
            assertTrue("XXX should not be set", Settings.has("xxx"));
            assertTrue("XXX should be length 1", Settings.getArray("xxx").length == 1L);
            Settings.add("XXX", "bbb");
            assertTrue("XXX should be length 2", Settings.getArray("xxx").length == 2L);
            System.err.println("Adds worked!");
            Settings.put("xxx", "null");
            assertTrue("Cleared xxx", !Settings.has("xxx"));
            assertTrue("Cleared xxx2", Settings.get("xxx") == null);
            org.junit.Assert.assertTrue("Cleared xxx3", Settings.getArray("xxx").length == 0L);
            System.err.println("Delete worked");
            Settings.put("xxx", "a,b,c");
            assertTrue("Reset xxx", Settings.getArray("xxx").length == 3L);
            System.err.println("Settings OK");
            assertTrue("Sugg1", Settings.get("xxx").equals("a,b,c"));
            Settings.suggest("xxx", "d,e,f");
            assertTrue("Sugg2", Settings.get("xxx").equals("a,b,c"));
            Settings.suggest("yyy", "d,e,f");
            assertTrue("Sugg3", Settings.get("yyy").equals("d,e,f"));
            Settings.suggest("yyy", "a,b,c");
            assertTrue("Sugg4", Settings.get("yyy").equals("d,e,f"));
            assertTrue("Sugg5", Settings.has("yyy"));
            Settings.put("yyy", "null");
            assertTrue("Sugg6", !Settings.has("yyy"));
            Settings.suggest("yyy", "abc");
            assertTrue("Sugg7", !Settings.has("yyy"));
            Settings.put("yyy", "abc");
            assertTrue("Sugg8", Settings.has("yyy"));
        }
    }

    @Test
    public void testPosit() throws Exception {

        base = new HCounter(9);
        base.down();

        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "90");
        currentSettings.put("coordinates", "galactic");
        currentSettings.put("pixels", "30");
        currentSettings.put("Sampler", "NN");
        currentSettings.put("position", "180.,0.");
        String orig = base.toString();
        test(true);

        currentSettings.remove("position");
        currentSettings.put("Lat", "0");
        currentSettings.put("Lon", "180");
        test(true);

        currentSettings.remove("lat");
        currentSettings.remove("lon");

        currentSettings.put("copywcs", orig + ".fits");
        test(true);
    }

    @Test
    public void testQLFormats() throws Exception {

        currentSettings.clear();
        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "360,180");
        currentSettings.put("coordinates", "galactic");
        currentSettings.put("pixels", "200,100");
        currentSettings.put("Sampler", "NN");
        currentSettings.put("position", "180.,0.");

        String[] formats = {"", "JPEG", "JPG", "GIF", "BMP", "TIFF", "PNG"};

        base = new HCounter(10);

        String stem = base + ".";
        base.down();
        if (included(base)) {
        
            doTests(formats, "quicklook", false);
            assertTrue("JPEG1", new File(stem + "1.jpg").exists());
            assertTrue("JPEG2", new File(stem + "2.jpg").exists());
            assertTrue("JPEG3", new File(stem + "3.jpg").exists());
            assertTrue("GIF", new File(stem + "4.gif").exists());
            assertTrue("BMP", new File(stem + "5.bmp").exists());
            assertTrue("TIFF", new File(stem + "6.tiff").exists());
            assertTrue("PNG", new File(stem + "7.png").exists());

            currentSettings.put("quicklook", "");
            currentSettings.put("nofits", "");
            stem = base.toString();
            test(false);
            assertTrue("Nofits test1", (new File(stem + ".jpg").exists()));
            assertTrue("Nofits test2", !(new File(stem + ".fits").exists()));
        }
    }

    @Test
    public void testGrid() throws Exception {

        currentSettings.clear();

        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "360,180");
        currentSettings.put("coordinates", "galactic");
        currentSettings.put("pixels", "400,200");
        currentSettings.put("Sampler", "NN");
        currentSettings.put("position", "0., 0.");
        currentSettings.put("nofits", "");

        currentSettings.put("grid", "");
        currentSettings.put("quicklook", "JPG");

        base = new HCounter(11);
        base.down();
        test(false);

        currentSettings.put("gridlabels", "");
        test(false);

        currentSettings.put("grid", "equatorial");
        test(false);

        currentSettings.put("grid", "");
        currentSettings.put("projection", "Tan");
        currentSettings.put("position", "45.,90.");
        currentSettings.put("pixels", "300");
        currentSettings.put("size", "60");

        currentSettings.remove("gridlabels");
        test(false);

        currentSettings.put("gridlabels", "");
        test(false);

        currentSettings.put("grid", "equatorial");
        test(false);

    }

    @Test
    public void testContour() throws Exception {

        System.err.println("Test contours");

        currentSettings.clear();

        currentSettings.put("Survey", "heao1a");
        currentSettings.put("projection", "Car");
        currentSettings.put("size", "360,180");
        currentSettings.put("coordinates", "galactic");
        currentSettings.put("pixels", "400,200");
        currentSettings.put("Sampler", "NN");
        currentSettings.put("position", "0., 0.");
        currentSettings.put("contour", "heao1a");
        currentSettings.put("quicklook", "jpg");

        base = new HCounter(12);
        base.down();
        test(false);

        currentSettings.put("contourSmooth", "7");
        test(false);

        currentSettings.remove("contourSmooth");
        currentSettings.put("contour", "heao1a:linear");
        test(false);

        currentSettings.put("contour", "heao1a:sqrt");
        test(false);

        currentSettings.put("contour", "heao1a:log:6");
        test(false);

        currentSettings.put("contour", "heao1a:log:6:1:1000");
        test(false);

        currentSettings.put("contour", "egrethard");
        test(false);

        currentSettings.put("contour", "egrethard:log:5:1.e-7:0.01");
        test(false);

        currentSettings.put("contourSmooth", "5");
        test(false);

        currentSettings.put("noContourPrint", "");
        test(false);
    }

    @Test
    public void testSmoothing() throws Exception {

        base = new HCounter(13);
        base.down();

        currentSettings.clear();
        currentSettings.put("survey", "user");
        currentSettings.put("userfile", "inputtest1.fits");
        currentSettings.put("CopyWCS", "inputtest1.fits");
        currentSettings.put("Smooth", "5");
        if (included(base)) {
            double[] data = getData();
            System.err.println("Smoothing 5x5 box:" + data[5 * 11 + 5]);
            assertEquals("5x5 box: in", data[5 * 11 + 5], 1. / 25, 0);
            assertEquals("5x5 box: in", data[5 * 11 + 6], 1. / 25, 0);
            assertEquals("5x5 box: in", data[5 * 11 + 7], 1. / 25, 0);
            assertEquals("5x5 box: in", data[6 * 11 + 5], 1. / 25, 0);
            assertEquals("5x5 box: in", data[7 * 11 + 5], 1. / 25, 0);
            assertEquals("5x5 box: out", data[8 * 11 + 5], 0., 0);
            assertEquals("5x5 box: out", data[5 * 11 + 8], 0., 0);
        }
        base.increment();

        currentSettings.put("Smooth", "5,1");
        if (included(base)) {
            double[] data = getData();
            assertEquals("5x5 box: in", data[5 * 11 + 5], 1. / 5, 0);
            assertEquals("5x5 box: in", data[5 * 11 + 6], 1. / 5, 0);
            assertEquals("5x5 box: in", data[5 * 11 + 7], 1. / 5, 0);
            assertEquals("5x5 box: out", data[6 * 11 + 5], 0., 0);
            assertEquals("5x5 box: out", data[5 * 11 + 8], 0., 0);
        }
        base.increment();

        currentSettings.put("Smooth", "1,5");
        if (included(base)) {
            double[] data = getData();
            assertEquals("5x5 box: in", data[5 * 11 + 5], 1. / 5, 0);
            assertEquals("5x5 box: in", data[6 * 11 + 5], 1. / 5, 0);
            assertEquals("5x5 box: in", data[7 * 11 + 5], 1. / 5, 0);
            assertEquals("5x5 box: out", data[5 * 11 + 6], 0., 0);
            assertEquals("5x5 box: out", data[8 * 11 + 5], 0., 0);
        }
        base.increment();

    }

    @Test
    public void testCatalogs() throws Exception {

        base = new HCounter(14);
        base.down();
        currentSettings.clear();
        currentSettings.put("survey", "rass-cnt broad");
        currentSettings.put("scaling", "log");
        currentSettings.put("size", "15");
        currentSettings.put("pixels", "500");
        currentSettings.put("catalog", "rosmaster");
        currentSettings.put("quicklook", "jpg");
        currentSettings.put("position", "0.,0.");
        currentSettings.put("coordinates", "Galactic");
        currentSettings.put("projection", "Car");
        currentSettings.remove("nofits");
        currentSettings.remove("min");
        currentSettings.remove("max");
        test(false);

        currentSettings.put("catalogids", "");
        if (test(false)) {

            currentSettings.put("catalogfile", "");
            String prefix = base.toString();
            test(false);
            assertTrue("tableexists", new File(prefix + ".fits.tab").exists());
            long l1 = new File(prefix + ".fits.tab").length();

            currentSettings.put("catalogradius", "5");
            String prefix2 = base.toString();
            test(false);
            long l2 = new File(prefix2 + ".fits.tab").length();

            assertTrue("radiusfilter", l2 < l1);

            currentSettings.put("catalogfilter", "instrument=hri");
            String prefix3 = base.toString();
            test(false);
            long l3 = new File(prefix3 + ".fits.tab").length();
            assertTrue("fieldfilter", l3 < l2);

            currentSettings.put("catalogfilter", "instrument=hri,exposure>20000");
            String prefix4 = base.toString();
            test(false);
            long l4 = new File(prefix4 + ".fits.tab").length();
            assertTrue("fieldfilter", l4 < l3);
            currentSettings.put("position", "289.95087909728574,64.35997524900246");
            currentSettings.put("survey", "dss");
            currentSettings.put("projection", "Tan");
            currentSettings.put("size", "0.1");
            currentSettings.remove("catalogFilter");
            currentSettings.remove("catalogRadius");
            currentSettings.remove("nofits");
            currentSettings.put("pixels", "300");
            currentSettings.remove("scale");
            currentSettings.put("catalog", "ned");
            test(false);
            currentSettings.put("catalog", "I/284");
            test(false);
            currentSettings.put("catalog", "http://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=rosmaster&");
            test(false);
            currentSettings.put("catalog", "ned,I/284,http://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=rosmaster&");
            test(false);
            currentSettings.put("catalog", "ned,I/284,http://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=rosmaster&");
            test(false);
            currentSettings.put("catalog", "ned");
            currentSettings.put("catalogfields", "");
            test(false);

            currentSettings.put(
                    "catalog", "rosmaster");
            currentSettings.put(
                    "catalogfields", "");
            currentSettings.put(
                    "catalogfile", "mycat.file");
            currentSettings.put(
                    "catalogcolumns", "instrument,exposure");
            test(false);
            assertTrue("Create cat file", new File("mycat.file.1").exists());
        }
    }

    @Test
    public void testLUT() throws Exception {

        currentSettings.clear();
        currentSettings.put("survey", "dss");
        currentSettings.put("quicklook", "jpg");
        currentSettings.put("position", "187.27791499999998,2.052388");
        currentSettings.put("Coordinates", "J2000");
        currentSettings.put("pixels", "500");
        currentSettings.put("nofits", "");
        currentSettings.remove("postprocessor");

        base = new HCounter(15);
        base.down();

        test(false);
        currentSettings.put("invert", "");
        test(false);

        currentSettings.remove("invert");
        currentSettings.put("lut", "fire");
        test(false);

        currentSettings.put("invert", "");
        test(false);
        currentSettings.remove("invert");
        currentSettings.remove("lut");
        currentSettings.put("coltab", "green-pink");
        test(false);
        currentSettings.remove("coltab");
        currentSettings.put("lut", "colortables/green-pink.bin");
        test(false);

    }

    @Test
    public void testRGB() throws Exception {

        currentSettings.clear();
        currentSettings.put("survey", "iras100,iras25,rass-cnt broad");
        currentSettings.put("position", "0.,0.");
        currentSettings.put("coordinates", "G");
        currentSettings.put("Pixels", "600,300");
        currentSettings.put("size", "40,20");
        currentSettings.put("rgb", "");
        currentSettings.put("quicklook", "JPEG");


        base = new HCounter(16);
        base.down();
        
        test(false);
        
        currentSettings.put("rgbsmooth", "1,1,5");
        test(false);
        
        currentSettings.put("rgbsmooth", "1,1,5");
        currentSettings.put("grid", "");
        currentSettings.put("gridlabels", "");
        
        test(false);
    }

    @Test
    public void testScaling() throws Exception {

        currentSettings.clear();
        currentSettings.put("survey", "iras100");
        currentSettings.put("position", "0.,0.");
        currentSettings.put("coordinates", "G");
        currentSettings.put("Pixels", "500");
        currentSettings.put("size", "10");
        currentSettings.put("quicklook", "JPEG");

        base = new HCounter(17);
        base.down();

        currentSettings.put("scaling", "log");
        test(false);

        currentSettings.put("scaling", "linear");
        test(false);

        currentSettings.put("scaling", "sqrt");
        test(false);
        
        Settings.put("scaling", "histeq");
        test(false);

        Settings.put("scaling", "log");
        Settings.put("min", "200");
        test(false);

        Settings.put("scaling", "log");
        Settings.put("max", "5000");
        test(false);
    }

    @Test
    public void testPlot() throws Exception {

        currentSettings.clear();
        currentSettings.put("survey", "iras100");
        currentSettings.put("position", "0.,0.");
        currentSettings.put("coordinates", "G");
        currentSettings.put("Pixels", "500");
        currentSettings.put("size", "10");
        currentSettings.put("quicklook", "JPEG");


        base = new HCounter(18);
        base.down();

        currentSettings.put("grid", "");
        currentSettings.put("gridlabels", "");
        currentSettings.put("catalog", "rosmaster");
        currentSettings.put("catalogids", "");
        test(false);

        currentSettings.put("plotscale", "2");
        test(false);

        currentSettings.put("plotscale", "3");
        currentSettings.put("plotfontsize", "20");
        test(false);

        currentSettings.put("plotcolor", "green");
        currentSettings.put("lut", "grays");
        test(false);

        currentSettings.put("lut", "fire");
        test(false);

        currentSettings.put("draw", "50 50,-50 -50,,50 -50,-50 50");
        test(false);

        currentSettings.remove("draw");
        currentSettings.put("drawfile", "plot1.drw");
        test(false);
        
        currentSettings.put("drawfile", "plot1.drw");
        currentSettings.put("drawangle", "45");
        test(false);

        currentSettings.remove("drawangle");
        currentSettings.put("drawfile", "plot2.drw");
        test(false);
    }
}
