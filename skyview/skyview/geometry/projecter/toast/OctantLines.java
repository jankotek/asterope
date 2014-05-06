

package skyview.geometry.projecter.toast;

/**
 * 
 * @author tmcglynn
 */
public class OctantLines {
    
    public static void main(String[] args) {
        
        int np = 100;
        if (args.length > 0) {
            np = Integer.parseInt(args[0]);
        }
        
        System.out.println("VECT");
        System.out.println("6 "+6*np+" 6");
        System.out.println(np+" "+np+" "+np+" "+np+" "+np+" "+np);
        System.out.println("1 1 1 1 1 1");
        double d = 90/(np-1);
        // Meridian 0        
        double[][] vec = getUnits(0, 0, 0, d, np);
        printUnits(vec);
        
        // Meridian 45 (bisects angle at pole)
        vec = getUnits(45, 0, 0, d, np);
        printUnits(vec);
        
        // Meridian 90
        vec = getUnits(90, 0, 0, d, np);
        printUnits(vec);
        
        //  Bisector of angle at 90,0.
        double s2 = 1./Math.sqrt(2);
        for (int i=0; i<np; i += 1) {
            double t0 =   s2*vec[i][0] + s2*vec[i][2];
            double t2 =  -s2*vec[i][0] + s2*vec[i][2];
            vec[i][0] = t0;
            vec[i][2] = t2;
        }
        printUnits(vec);
        
        
        // Equator
        vec = getUnits(0, d, 0, 0, np);
        printUnits(vec);
        
        //  Bisector of angle at 0,0.
        for (int i=0; i<np; i += 1) {
            double t1 =  s2*vec[i][1] - s2*vec[i][2];
            double t2 =  s2*vec[i][1] + s2*vec[i][2];
            vec[i][1] = t1;
            vec[i][2] = t2;
        }
        printUnits(vec);
        
        // Color
        System.out.println("0 0 0 1");
        System.out.println("0.5 0.5 0.5 1");
        System.out.println("0 0 0 1");
        System.out.println("0.5 0.5 0.5 1");
        System.out.println("0 0 0 1");
        System.out.println("0.5 0.5 0.5 1");
    }
    
    static void printUnits(double[][] vec) {
        for (int i=0; i<vec.length; i += 1) {
            double[] x = vec[i];
            System.out.printf("%f %f %f  ", x[0], x[1], x[2]);
        }
        System.out.println();
    }
    
    static double[][] getUnits(double l0, double dl, double b0, double db, int n) {
        double[][] vec = new double[n][3];
        l0 = Math.toRadians(l0);
        dl = Math.toRadians(dl);
        b0 = Math.toRadians(b0);
        db = Math.toRadians(db);
        
        for (int i=0; i<n; i += 1) {
            double[] x = vec[i];
            double lc = l0;
            if (b0 > 90) {
                lc = Math.PI + lc;
            }
            x[0] = Math.cos(lc)*Math.cos(b0);
            x[1] = Math.sin(lc)*Math.cos(b0);
            x[2] = Math.sin(b0);
            l0 += dl;
            b0 += db;
        }
        return vec;
    }
}
