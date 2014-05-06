package skyview.geometry.projecter;

import java.util.Arrays;
import skyview.executive.Settings;
import skyview.geometry.Deprojecter;
import skyview.geometry.Projecter;
import skyview.geometry.Transformer;
import skyview.geometry.Util;

/**
 *
 * @author tmcglynn
 */
public class Zpn extends Projecter {

    private double[] params;

    /**
     * Get a name for the component
     */
    public String getName() {
        return "Zpn";
    }

    protected double[] getParams() {
        return params;
    }
    
    protected void setParams(double[] newParams) {        
        params = newParams.clone();
    }
    
    public Zpn() {
        // Get the expansion parameters.
        // These should be passed as settings PROJPn
        int maxParm = -1;
        for (int i=1; i<21; i += 1) {
            if (Settings.has("_PROJP"+i)) {
                String sval = null;
                try {
                    sval = Settings.get("_PROJP"+i).trim();
                    double val = Double.parseDouble(sval);
                    if (val != 0) {
                        maxParm = i;
                    }
                } catch (Exception e) {
                    System.err.println("Invalid ZPN polynomial coefficient ignored for power "+i+": '"+sval+"'");
                }
            }
        }
        if (maxParm <= 0) {
            throw new IllegalArgumentException("No polynomial coefficients found for ZPN projection");
        }
        
        params = new double[maxParm];
        for (int i=1; i<=maxParm; i += 1) {
            if (Settings.has("_PROJP"+i)) {
                try {
                    double val = Double.parseDouble(Settings.get("_PROJP"+i));
                    params[i-1] = val;
                } catch (Exception e) {
                    // Noted above, ignored here.
                }
            }
        }
    }
    
    /**
     * Get a description for the component
     */
    public String getDescription() {
        return "Zenithal Polynomial projecter";
    }

    /**
     * Get the inverse of the transformation
     */
    public Deprojecter inverse() {
        return new ZpnDeproj();
    }

    /**
     * Is this an inverse of some other transformation?
     */
    public boolean isInverse(Transformer t) {
        if (t.getName().equals("ZpnDeproj")) {
            ZpnDeproj zt = (ZpnDeproj) t;
            return Arrays.equals(zt.getParams(), params);
        } else {
            return false;
        }
    }

    /**
     * Project a point from the sphere to the plane.
     *
     * @param sphere a double[3] unit vector
     * @param plane a double[2] preallocated vector.
     */
    public final void transform(double[] sphere, double[] plane) {

        if (Double.isNaN(sphere[2])) {
            plane[0] = Double.NaN;
            plane[1] = Double.NaN;
        } else {
            double theta = Math.acos(sphere[2]);
            double x = value(theta);
            double num = x;
            if (num < 0) {
                num = 0;
            }
            double denom = sphere[0] * sphere[0] + sphere[1] * sphere[1];
            if (denom == 0) {
                plane[0] = 0;
                plane[1] = 0;
            } else {
                double ratio = num / Math.sqrt(denom);
                plane[0] = ratio * sphere[0];
                plane[1] = ratio * sphere[1];
            }            
        }
    }
    
    private double value(double theta) {
        double x = 0;
        for (int i = params.length-1; i >= 0; i -= 1) {
            x *= theta;
            x += params[i];
        }
        x *= theta;  // We don't have a 0 term so we need to do another multiply.
        return x;
    }
    
    private double deriv(double theta) {
        double x = 0;
        for (int i=params.length-1; i >= 0; i -= 1) {
            x *= theta;
            x += (i+1)*params[i];
        }
        return x;    
    }

    public boolean validPosition(double[] plane) {
        return super.validPosition(plane);
    }

    public class ZpnDeproj extends Deprojecter {

        /**
         * Get the name of the component
         */
        public String getName() {
            return "ZpnDeproj";
        }
        private boolean messageWritten  = false;

        /**
         * Get the description of the compontent
         */
        public String getDescription() {
            return "Zenithal polynomial deprojecter";
        }

        /**
         * Get the inverse transformation
         */
        public Projecter inverse() {
            return Zpn.this;
        }

        /**
         * Is this an inverse of some other transformation?
         */
        public boolean isInverse(Transformer t) {
            if (t.getName().equals("Zpn")) {
                Zpn zt = (Zpn) t;
                return Arrays.equals(zt.getParams(), params);
            } else {
                return false;
            }
        }
        
        
        protected double[] getParams() {
            return Zpn.this.getParams();
        }

        /**
         * Deproject a point from the plane to the sphere.
         *
         * @param plane a double[2] vector in the tangent plane.
         * @param spehre a preallocated double[3] vector.
         */
        public final void transform(double[] plane, double[] sphere) {


            if (!validPosition(plane)) {
                sphere[0] = Double.NaN;
                sphere[1] = Double.NaN;
                sphere[2] = Double.NaN;

            } else {
                
                double r = Math.sqrt(plane[0] * plane[0] + plane[1] * plane[1]);
                
                // Need to convert back but we have a polynomial, so
                // we use iteration.
                double theta = Math.PI/2 - invert(r);
                sphere[2] = Math.cos(theta);
                double ratio = (1 - sphere[2] * sphere[2]);
                if (ratio > 0) {
                    ratio = Math.sqrt(ratio) / r;
                } else {
                    ratio = 0;
                }
                sphere[0] = ratio * plane[0];
                sphere[1] = ratio * plane[1];
            }
        }
        
        private double invert(double r) {
            double tol = 1.e-12;
            int iter = 0;
            double x = r;
            while (iter < 10) {
                double val   = value(x);
                double deriv = deriv(x);
                double err = r-val;
                if (Math.abs(err) < tol) {
                    break;                    
                }
                x += err/deriv;
                iter += 1;
            }
            if (iter >= 10) {
                if (!messageWritten) {
                    System.err.println("No convergence in ZPN inverse");
                    messageWritten = true;
                }
            }
            return x;
        }
    }
}
