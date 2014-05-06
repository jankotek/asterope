package skyview.tile;

import java.util.ArrayList;
import java.util.List;
import skyview.executive.Settings;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.Projection;
import skyview.geometry.Scaler;
import skyview.geometry.TransformationException;
import skyview.geometry.WCS;
import skyview.survey.Image;

/**
 *
 * @author tmcglynn
 */
public class TanTiler implements Tiler {

    /**
     * Center separation in degrees
     */
    private double delta = 1;
    private boolean poles = false;
    private double overlap = 0.1;
    private int nx = 1024;
    private int ny;
    private double xScale = 1; // Arc second
    private double yScale;
    private CoordinateSystem csys = CoordinateSystem.factory("J2000");
    /**
     * Centers of tiles
     */
    private List<double[]> centers;

    public void initialize() {
        if (Settings.has("delta")) {
            delta = Double.parseDouble(Settings.get("delta"));
        }
        poles = Settings.has("Poles");
        if (Settings.has("overlap")) {
            overlap = Double.parseDouble(Settings.get("overlap"));
        }
        if (Settings.has("nx")) {
            nx = Integer.parseInt(Settings.get("nx"));
        }
        if (Settings.has("ny")) {
            ny = Integer.parseInt(Settings.get("ny"));
        } else {
            ny = nx;
        }
        if (Settings.has("xscale")) {
            xScale = Double.parseDouble(Settings.get("xscale"));
        }
        if (Settings.has("yscale")) {
            yScale = Double.parseDouble(Settings.get("yscale"));
        } else {
            yScale = xScale;
        }
    }

    public int getImageCount() {
        if (centers == null) {
            generateCenters();
        }
        return centers.size();
    }

    public List<double[]> getCenters() {
        if (centers == null) {
            generateCenters();
        }
        return centers;
    }

    private void generateCenters() {
        double nDec = 180 / delta;
        int nD = (int) Math.ceil(nDec);
        double decDelta = 180 / nD;
        if (poles) {
            // If the poles are to have their own tiles,
            // then each of the pole tiles only contributes half its
            // width, so we get one extra range.
            nD += 1;
        }

        int cnt = 0;
        centers = new ArrayList<double[]>();
        for (int i = 0; i < nD; i += 1) {
            double dec = -90 + i * decDelta;
            if (poles && (i == 0 || i == nD - 1)) {
                centers.add(new double[]{dec, 0});
            } else {
                if (!poles) {
                    dec += decDelta / 2;
                }
                double tdec = Math.abs(dec);
                if (tdec > 0) {
                    tdec = Math.max(0., tdec - delta / 2);
                }
                // Find minimum of abs(dec) for tile.  This
                // helps us to know how many tiles we need to girdle
                // this ring.
                double tr = Math.toRadians(tdec);

                double size = 360 * Math.cos(tr);
                double rab = size / delta;
                int nR = (int) Math.ceil(rab);
                if (nR < 8) {
                    nR = 8;
                }

                double dRA = 360 / nR;
                // Offset every other row.
                double raOff = (i % 2) * dRA / 2;
                for (int j = 0; j < nR; j += 1) {
                    centers.add(new double[]{dec, j * dRA + raOff});
                }
            }
        }
    }


    
    public Image getImage(int i) {
        if (centers == null) {
            generateCenters();
        }
        if (i < 0 || i >= centers.size()) {
            throw new IllegalArgumentException("Requested image out of range");
        }
        double[] data = centers.get(i).clone();
        data[0] = Math.toRadians(data[0]);
        data[1] = Math.toRadians(data[1]);
        try {
            Projection p = new Projection("Tan", data);

            double xs = Math.toRadians(xScale);
            double ys = Math.toRadians(yScale);
            double x0 = nx / 2;
            double y0 = ny / 2;
            Scaler s = new Scaler(-ys * x0, -ys * y0,
                    xs, 0, 0, ys);
            WCS w = new WCS(csys, p, s);
            Image im = new Image(new double[nx*ny], w, nx, ny);
            return im;
        } catch (TransformationException pe) {
            System.err.println("Got execption:" + pe);
            throw new Error("Unexpected exception", pe);
        }

    }
    
    public int[] getDimensions() {
        return new int[]{nx,ny};
    }

    public static void main(String[] args) {
        Settings.put("delta", args[0]);
        if (args.length > 1) {
            Settings.put("poles", "1");
        }

        Tiler t = new TanTiler();
        t.initialize();
        List<double[]> centers = t.getCenters();
//        int[] dim = t.getDimensions();
        for (int i = 0; i < centers.size(); i += 1) {
            System.out.printf("%5d: %10.5f %10.5f\n", i, centers.get(i)[0], centers.get(i)[1]);
            Image im = t.getImage(i);
            
        }
    }
}
