

package skyview.geometry.projecter.toast;

import skyview.geometry.Util;
import skyview.geometry.projecter.Toa;

/**
 * 
 * 
 * @author tmcglynn
 */
public class ToaGrid {
    
    public static void main(String[] args) {
        int start = Integer.parseInt(args[0]);
        int mx = (int)Math.pow(2, start);
        int ny    = Integer.parseInt(args[1]);
        int nx    = Integer.parseInt(args[2]);
        if (nx >= mx || ny >= mx) {
            System.err.println("Grid outside range");
            System.exit(-1);
        }
        ny = (mx-1)-ny;
        int sub   = Integer.parseInt(args[3]);
        int ndim = (int)Math.pow(2,sub) + 1;
        int crd = -1;
        if (args.length > 4) {
            crd = Integer.parseInt(args[4]); 
        }
        
        Toa t = new Toa();
        double[][][] values = t.tile(start, nx, ny, sub);
        for (int i=0; i<ndim; i += 1) {
            for (int j=0; j<ndim; j += 1) {
                double[] unit = values[i][j];
                double[] coords = Util.coord(unit);
                if (crd == 0 || crd == 1) {
                     System.out.printf("%24.17f ", Math.toDegrees(coords[crd])); 
                } else {
                    System.out.printf("%12.7f %12.7f  ", Math.toDegrees(coords[0]), Math.toDegrees(coords[1]));
                }
            }
            System.out.println();
        }
    }  
    
    private static double angle(double[] x, double[] y) {
        double xsd = dot(x,x);
        double ysd = dot(y,y);
        double val = dot(x,y)/Math.sqrt(xsd)/Math.sqrt(ysd);
        if (val > 1) {
            return 0;
        } else if (val < -1) {
            return Math.PI;
        }
        return Math.acos(val);
    }
    
    private static double dot(double[] x, double[] y) {
        return x[0]*y[0] + x[1]*y[1] + x[2]*y[2];
    }
    
    private static double[] cross(double[] x, double[] y) {
        return new double[] {
            x[1]*y[2]-x[2]*y[1],
           -x[0]*y[2]+x[2]*y[0],
            x[0]*y[1]-x[1]*y[0]
        };
    }
}
