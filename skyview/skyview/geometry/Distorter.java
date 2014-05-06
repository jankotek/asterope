package skyview.geometry;


/** This class defines a non-linear distortion in the image plane.
    Normally the forward distortion converts from a fiducial
    projection plane to some distorted coordinates.  The reverse
    distortion transforms from the distorted coordinates back
    to the fiducial coordinates.
  */
public abstract class Distorter extends Transformer implements skyview.Component {
    
    /** A name for this object */
    public abstract String getName();
    
    /** What does this object do? */
    public abstract String getDescription();
    
    public abstract Distorter inverse();
    
    /** What is the output dimensionality of a Distorter? */
    protected int getOutputDimension() {
	return 2;
    }
    
    /** What is the input dimensionality of a Distorter? */
    protected int getInputDimension() {
	return 2;
    }
    
    /** Get the local Jacobian for the distortion.
     *  This implementation defers the calculation to the
     *  inverse distorter.  Clearly this will need to be
     *  overriden in either the forward or backward distorter.
     * @param pix  The input position.
     * @return The Jabobian matrix.
     */
    public double[][] jacobian(double[] pix) {
        
        // Assume the simple functions run in the other direction.
        // We will compute the matrix going that way and then invert it.
        
        // Get the corresponding position for the reverse transformation.
        double[] opix  = inverse().transform(pix);
        
        // Get the Jacobian for the inverse. 
        double[][] mat = inverse().jacobian(opix);
        
        // Now invert it.
        double det = mat[0][0]*mat[1][1] - mat[0][1]*mat[1][0];

        double[][] xmat = new double[2][2];        
        
        xmat[0][0] =   mat[1][1]/det;
        xmat[0][1] = - mat[0][1]/det;
        xmat[1][0] = - mat[1][0]/det;
        xmat[1][1] =   mat[0][0]/det;
        
        return xmat;        
    }

}
