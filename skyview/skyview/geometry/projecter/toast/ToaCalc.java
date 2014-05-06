

package skyview.geometry.projecter.toast;

import skyview.geometry.Util;
import skyview.geometry.projecter.Toa;

/**
 * 
 * @author tmcglynn
 */
public class ToaCalc {
    
    public static void main(String[] args) {
        int level = 5;
        if (args.length > 0) {
            level = Integer.parseInt(args[0]);
        }
        int n = (int) Math.pow(2,level);
        double sz = 4*Math.PI/(n*n);
        System.out.printf("%d  %d  %f\n", level, n, sz);
        Toa t = new Toa();
        double[][][] values = t.tile(0, 0, 0, level);
        int ny = values.length;
        int nx = values[0].length;
        double totalArea = 0;
        double totalCount = 0;
        double missCount = 0;
        double rat = n;
        double drvavg = 0;
        for (int i=0; i<ny-1; i += 1) {
            int ip = i+1;
            for (int j=0; j<nx-1; j += 1) {
                int jp = j+1;
                double[] p00 = values[i][j];
                double[] p01 = values[i][jp];
                double[] p10 = values[ip][j];
                double[] p11 = values[ip][jp];
                
                // Now calculate the cross-products of consecutive elements.
                double[] x00_01 = cross(p00,p01);
                double[] x01_11 = cross(p01,p11);
                double[] x11_10 = cross(p11,p10);
                double[] x10_00 = cross(p10,p00);
                double a0 = angle(x00_01, x01_11);
                double a1 = angle(x01_11, x11_10);
                double a2 = angle(x11_10, x10_00);
                double a3 = angle(x10_00, x00_01);
                double sum = a0 + a1 + a2 + a3;
                double[] c0 = Util.coord(values[i][j]);
                double[] cx = Util.coord(values[i][jp]);
                double[] cy = Util.coord(values[ip][j]);
                double dx0x = Math.abs(c0[0]-cx[0])*n;
                double dx0y = Math.abs(c0[0]-cy[0])*n;
                double dy0x = Math.abs(c0[1]-cx[1])*n;
                double dy0y = Math.abs(c0[1]-cy[1])*n;
                double delta = 0;
                
                double[] cxm = null;
                double[] cym = null;
                double dx0xm = 0;
                if (i > 0 && j > 0) {
                    cxm = Util.coord(values[i][j-1]);
                    cym = Util.coord(values[i-1][j]);
                    dx0xm = Math.abs(cxm[0]-c0[0])*n;
                    delta = dx0xm - dx0x; 
                } else {
                    cxm = new double[]{0,0};
                    cym = new double[]{0,0};
                }
                // Multiply by n because we should be dividing by spacing ~ 1/n.
                double drv = Math.sqrt(dx0x*dx0x + dx0y*dx0y + dy0x*dy0x+ dy0y*dy0y);
                drvavg += drv;
                
                  
                if (sum == sum) {
                    totalArea += 2*Math.PI - sum;
                    totalCount += 1;
                } else {
                    System.err.println("NAN check:");
                    Toa.show(" x00:",p00);
                    Toa.show(" x01:",p01);
                    Toa.show(" x10:",p10);
                    Toa.show(" x11:",p11);
                    Toa.show(" c00:", x00_01);
                    Toa.show(" c00:", x01_11);
                    Toa.show(" c00:", x11_10);
                    Toa.show(" c00:", x10_00);
                    System.err.println("Angles: "+a0+" "+a1+" "+a2+" "+a3);
                    missCount += 1;
                }
                
                double area = 2*Math.PI-sum;
                System.out.printf("%5d %5d %.10f %.5f %.8f %.8f %.8f %.8f %.8f %.8f %.8f\n", i,j,area,area/sz,drv, dx0xm,dx0x, cxm[0],c0[0],cx[0],delta);
            }
        }
        double avg = totalArea/totalCount;
        double xSum = totalArea + missCount*avg;
        System.out.println("Total is:"+totalArea+" "+totalCount+" "+missCount+" "+xSum+" "+drvavg/n/n);
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
