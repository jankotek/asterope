

package skyview.geometry.distorter;

import nom.tam.fits.Header;
import skyview.executive.Settings;
import skyview.geometry.Distorter;
import skyview.geometry.Transformer;

/**
 * Implement the SIP convention for distorting a standard projection.
 * 
 * @author tmcglynn
 */
public class SIPDistorter extends Distorter {

    // Coefficients of the distortion.  Note that
    // the first index does not include constant or linear terms.
    private double  coefx[][];
    private double  coefy[][];
    
    // Coefficients of the inverse of the distortion.
    // No constant terms are included.
    private double icoefx[][];
    private double icoefy[][];
    
    // The coefficients of the derivatives of the distortion.
    // These are calculated when we do not have an inverse distortion given
    // or when we choose to invert numerically.
    private double dcoefxx[][];
    private double dcoefxy[][];
    private double dcoefyx[][];
    private double dcoefyy[][];
    
    // The orders of the coefficients.
    private int ncoefx;
    private int ncoefy;
    private int nicoefx;
    private int nicoefy;
    
    // If this is true, then we will use the coefficients of the
    // inverse distortion.  If false we will numerically invert
    // the distortion and will use the derivatives of the distortion.
    private boolean invert = false;

    // Maximum number of iterations to use in inverse distortion.
    private static int MAX_ITER    = 10;
    
    // Maximum number of non-convergence messages to display.
    private static int MAX_DISPLAY = 3;
    
    // Tolerance for the offset.
    private static double OFFSET_TOLERANCE_SQ = 1.e-20;
    
    public String getName() {
        return "SIPDistorter";
    }
    public String getDescription() {
        return "Distorter implement SIP convention";
    }
    public Distorter inverse() {
        // This will inherit the same parameters.
        return new SIPUndistorter();
    }
    
    /** Create a distorter from a FITS header.
     *  We will look for A_ORDER, B_ORDER, An_m and Bn_m
     *  keywords.  If AP_ORDER and BP_ORDER are found
     *  and the SIPUseInversion setting is set, then
     *  we will also look for APn_m and BPn_m for
     *  the inverse distortion.
     */
    public SIPDistorter(Header h) {
        try {
            ncoefx = h.getIntValue("A_ORDER", -1);
            ncoefy = h.getIntValue("B_ORDER", -1);
            if (ncoefx < 0 || ncoefy < 0) {
                throw new IllegalArgumentException("No coefficients defined");
            }
            nicoefx = h.getIntValue("AP_ORDER", -1);
            nicoefy = h.getIntValue("BP_ORDER", -1);
            if (nicoefx < 0 || nicoefy < 0) {
                nicoefx = 0;
                nicoefy = 0;
                invert = false;
            } else {
                invert = Settings.has("SIPUseInversion");
            }
            coefx = new double[ncoefx-1][];  // Don't need 0 and first order
            coefy = new double[ncoefy-1][];
            for (int order = 2; order <= ncoefx; order += 1) {
                coefx[order-2] = getCoefs(h, "A", order);                
            }
            for (int order = 2; order <= ncoefy; order += 1) {
                coefy[order-2] = getCoefs(h, "B", order);
            }
            
            if (invert) {
                icoefx = new double[nicoefx][];  // Done need 0 order
                icoefx = new double[nicoefy][];
                for (int order=1; order<nicoefx; order += 1) {
                    icoefx[order-1] = getCoefs(h, "AP", order);
                }
                for (int order=1; order<nicoefy; order += 1) {
                    icoefy[order-1] = getCoefs(h, "BP", order);
                }
            } else {
                getDeriv();            
            }  
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid FITS header: "+e);
        }
    }
    
    /** Get the coefficients for a given prefix (A,B, AP, BP) */
    private double[] getCoefs(Header h, String prefix, int order) {
        double[] coefs = new double[order+1];
        for (int y=0; y<=order; y += 1) {
            int x = order-y;
            coefs[y] = h.getDoubleValue(prefix+x+"_"+y);
        }
        return coefs;
    }
    
    /** Calculate the derivatives of the coefficients with regard to the input variables.
     *  The derivatives will be one order less, but since there is no constant terms
     *  the first dimension will be the same.
     */
    private void getDeriv() {
        dcoefxx = new double[ncoefx-1][];
        dcoefxy = new double[ncoefx-1][];
        
        dcoefyx = new double[ncoefy-1][];
        dcoefyy = new double[ncoefy-1][];
        System.err.println("Looking at coefficiencts:"+ncoefx+" "+ncoefy);
        for (int o=2; o<=ncoefx; o += 1) {
            // We lose one term where we differentiate the pure x^n term with respect to
            // y and vice versa.
            dcoefxx[o-2] = new double[o];
            dcoefxy[o-2] = new double[o];
            for (int y=0; y <= o-1; y += 1) {
                System.err.println("Looking at deriv:"+o+" "+y+" ");
                System.err.println("Lens: "+dcoefxx.length+" "+dcoefxy.length);
                System.err.println("Lens2:"+dcoefxx[o-2].length+" "+dcoefxy[o-2].length);
                // The x^(o-1) term coulds from the x^o term for the x partial
                // and from the x^(o-1)y term for the y partial.
                dcoefxx[o-2][y] = (o-y)*coefx[o-2][y];
                dcoefxy[o-2][y] = (y+1)*coefx[o-2][y+1];
            }            
        }
        for (int o=2; o<=ncoefy; o += 1) {
            // We lose one term where we differentiate the pure x^n term with respect to
            // y and vice versa.
            dcoefyx[o-2] = new double[o];
            dcoefyy[o-2] = new double[o];
            for (int y=0; y <= o-1; y += 1) {
                dcoefyx[o-2][y] = y*coefy[o-2][y];
                dcoefyy[o-2][y] = (y+1)*coefy[o-2][y+1];
            }            
        }
    }
    
    /** Explicitly set the coefficients for the distorter. 
     *  We infer the order from the input arrays.  If the
     *  undistort's arguments are given we assume that
     *  the user wishes to use the inverse distortion.  If null
     *  then we use numerical inversion of the distortion.
     */
    public SIPDistorter(double[][] distortx, double[][] distorty, 
                        double[][] undistortx, double[][] undistorty) {

        coefx  = distortx;
        coefy  = distorty;
        ncoefx = coefx.length+1;  // No constant or linear terms
        ncoefy = coefy.length+1;
                
        icoefx = undistortx;
        icoefy = undistorty;
        if (icoefx != null) {
            invert = true;
            nicoefx = icoefx.length;
            nicoefy = icoefy.length;
        } else {
            invert = false;
            nicoefx = 0;
            nicoefy = 0;
            getDeriv();
        }
    }

    /** Calculate the distorted coordinates given the standard coordinates.
     *  Note that the coefficients only give the distortion, so we need to
     *  add in the inputs.
     */
    public void transform(double[] in, double[] out) {
        double dx = sipForward(in, coefx, ncoefx, 2);
        double dy = sipForward(in, coefy, ncoefy, 2);
        out[0] = in[0] + dx;
        out[1] = in[1] + dy;
    }
    
    /** Calculate the forward polynomial.
     *  @param in A two-vector giving the input x,y
     *  @param coef An [order-start][o+1] array of coefficients.
     *  The first start orders are assumed to be 0
     *  For a given order, o, there are o+1 coefficients.
     *  E.g., if we have order=4 and start=2 then the dimensionality
     *  of coef is [[3],[4],[5]].
     *  The coefficients within each order are given in decreasing
     *  powers of x.  I.e., the first term is the x^o terms then next is x^(o-1)y and
     *  so forth.
     *  @param order The maximum order of coefficients.
     *  @param start The minimum order in for which coefficients are non-zero.
     *  The standard transformation includes constants and linear terms, so the forward
     *  transformation has start=2.  The inverse transformation may have linear
     *  terms so start=1.  
     *  @return The computed value for the polynomial expansion.
     */
    private double sipForward(double[] in, double[][] coef, int order, int start) {

        double val = 0;
        
        double x = in[0];
        double y = in[1];

        // Rat will add one term of y and delete one term of x.
        if (Math.abs(x) > 1.e-15) {
            double rat = y/x;
        
            for (int o=start; o <= order; o += 1) {
                double xy = Math.pow(x, o);
                for (int iy = 0; iy <= o; iy += 1) {
                    val += coef[o-start][iy] * xy;
                    xy *= rat;
                }
            }           
        } else {
            // Only the y^n terms matter since x is 0.
            for (int o=start; o<= order; o += 1) {
                val += Math.pow(y, o)*coef[o-start][o];                
            }
        }
        return val;
    }

    /** Is another transformer the inverse of this one?
     *  True only if it was created as the inverse of this transformation.
     */
    public boolean isInverse(Transformer trans) {
        try {
            return trans.inverse().equals(this);
        } catch (Exception e) {
            return false;
        }
    }
    
    

    /** Create the inverse distorter for the main class here. */
    public class SIPUndistorter extends Distorter {
                
        public Distorter inverse() {
            return (SIPDistorter.this);
        }
        
        public String getName() {
            return "SIPUndistorter";            
        }
        
        public String getDescription() {
            return "Polynomial undistorter";
        }

        public void transform(double[] in, double[] out) {
            double dx;
            double dy;
            if (invert) {
                dx = sipForward(in, icoefx, nicoefx, 1);
                dy = sipForward(in, icoefy, nicoefy, 1);
                out[0] = in[0] + dx;
                out[1] = in[1] + dy;
            } else {
                sipReverse(in, out);
            }
        }
        
        /** Invert the forward transformation numerically */     
        private void sipReverse(double[] in, double[] out) {
            
            // We are given an input position [x,y] and functions g(x,y), and h(x,y)
            // which give an offset from the 'true' projection position, and the
            // measured position.  I.e., g and h are the distortion terms.
            // So given so [g(x,y),h(x,y)] gives the distortion at position [x,y].
            // We want to find the position [x',y'] such that
            //   [x'+g(x',y'),y'+h(x',y')] = [x,y]    I.e.,
            //   [x',y'] = [x-g(x',y'),y-h(x',y')]
            // If we have the partial derivatives of the distortion
            //     [[ dg/dx, dg/dy], [dh/dx, dh/dy]]
            // We start by finding the distortion nd the partial derivatives
            // at the input point and using that (by inverting the matrix of partial
            // derivatives) to find an estimated point [x0', y0'] for [x',y']
            // and then recurse until we are close enough.  This assumes that distortion
            // terms are small.
            
            double x  = in[0];
            double y  = in[1];
            
            out[0] = in[0];
            out[1] = in[1];
            
            int iter = 0;     // Limit iterations to 10
            int displayed = 0;
            while (true) {
                if (iter > MAX_ITER) {
                    if (displayed < MAX_DISPLAY) {
                        displayed += 1;
                        System.err.println("No convergence for sipReverse at:"+in[0]+","+in[1]);                        
                    }
                    return;
                }
                iter += 1;
                double dx = out[0]+sipForward(out, coefx, ncoefx, 2) - x;
                double dy = out[1]+sipForward(out, coefy, ncoefy, 2) - y;
                System.out.println("X,y, dx, dy:"+x+" "+y+"   "+dx+" "+dy);
                // Could use the distance, but we'll use the maximum offset in either
                // direction.
                double offset_sq = dx*dx+dy*dy; 
                System.err.println("Tolerance:"+offset_sq);
                if (offset_sq < OFFSET_TOLERANCE_SQ) {
                    return;
                }
                // Not close enough.  Now we calculate the partial derivatives
                double dgdx = sipForward(out, dcoefxx, ncoefx-1, 1);
                double dgdy = sipForward(out, dcoefxy, ncoefx-1, 1);
                double dhdx = sipForward(out, dcoefyx, ncoefy-1, 1);
                double dhdy = sipForward(out, dcoefyy, ncoefy-1, 1);
                System.err.println("Derivs:"+dgdx+" "+dgdy+" "+dhdx+" "+dhdy);
                // 
                out[0] = out[0] - dgdx - dgdy*dy;
                out[1] = out[1] - dhdx*dx - dhdy*dy;
                System.err.println("Next x,y:"+out[0]+" "+out[1]);
            }
        }

        public boolean isInverse(Transformer trans) {
            try {
                return trans.inverse().equals(this);
            } catch (Exception e) {
                return false;
            }
        }   
    }
    
    public static void main(String[] args) {
        double x = Double.parseDouble(args[0]);
        double y = Double.parseDouble(args[1]);
        
        SIPDistorter s = new SIPDistorter(
                new double[][]{{.004, 0., .000}},
                new double[][]{{.000, 0., .002}},
                null,
                null
                );
        double[] in = {x,y};
        double[] out = new double[2];
        s.transform(in, out);
        
        System.out.println("x,y:"+x+" "+y+" -> "+out[0]+" "+out[1]);
        
        s.inverse().transform(out, in);
        System.out.println("x,yp:"+out[0]+" "+out[1]+" -> "+in[0]+" "+in[1]);
    }
}
