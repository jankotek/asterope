

package skyview.geometry.projecter.toast;

import skyview.geometry.Util;
import skyview.geometry.projecter.Toa;

/**
 * 
 * 
 * @author tmcglynn
 */
public class ToaDeriv {
    
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
        int ndim = (int)Math.pow(2,sub);
        int crd = 0;
        if (args.length > 4) {
            crd = Integer.parseInt(args[4]); 
        }
        int dcrd = 0;
        if (args.length > 5) {
            dcrd = Integer.parseInt(args[5]);
        }
        int xlim = ndim;
        int ylim = ndim;
        int xdel = 0;
        int ydel = 0;
        
        if (dcrd == 1) {
            xlim += 1;
            ydel = 1;
        } else {
            ylim += 1;
            xdel = 1;
        }
        
        double del = Math.pow(2, -sub-start);
        System.out.println("Step size="+1/del+" "+del);
        
                
        Toa t = new Toa();
        double[][][] values = t.tile(start, nx, ny, sub);
        int    cnt = 0;
        double sum = 0;
        for (int i=ydel; i<ylim; i += 1) {
//            for (int j=0; j<xlim; j += 1) {
//                double[] unit = values[i][j];
//                double[] coords = Util.coord(unit);
//                System.out.printf("%8.4f ", Math.toDegrees(coords[crd])); 
//            }
            System.out.println();
            for (int j=xdel; j<xlim; j += 1) {                
                double[] unit = values[i][j];
                double[] coords = Util.coord(unit);
                unit = values[i+ydel][j+xdel];
                double[] coords2 = Util.coord(unit);
                double res = Math.toDegrees(coords[crd]-coords2[crd])/del;
                System.out.printf("%8.4f ", Math.toDegrees(coords[crd]));
            }
            System.out.println();
            for (int j=xdel; j<xlim; j += 1) {                
                double[] unit = values[i][j];
                double[] coords = Util.coord(unit);
                unit = values[i+ydel][j+xdel];
                double[] coords2 = Util.coord(unit);
                double res = Math.toDegrees(coords2[crd]-coords[crd])/del;
                System.out.printf("%8.4f ", res);
            }
            System.out.println();
            for (int j=xdel; j<xlim; j += 1) {                
                double[] unit = values[i][j];
                double[] coords = Util.coord(unit);
                unit = values[i+ydel][j+xdel];
                double[] coords2 = Util.coord(unit);
                unit = values[i-ydel][j-xdel];
                double[] coords0 = Util.coord(unit);
                double res = Math.toDegrees(coords0[crd]+coords2[crd]-2*coords[crd])/del;
                System.out.printf("%8.4f ", res);
                cnt += 1;
                sum += Math.abs(res);
            }
            System.out.println();

        }
        System.out.println("Number of deltas:"+cnt+" Average: "+ (sum/cnt));
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
