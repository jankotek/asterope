package skyview.geometry.projecter;

import skyview.executive.Settings;
import skyview.geometry.Transformer;
import skyview.geometry.Deprojecter;
import skyview.geometry.Rotater;
import skyview.geometry.Scaler;
import skyview.geometry.TransformationException;
import skyview.geometry.Util;

/** This class provides for the
 *  translation between coordinates and the Tiled Octahedral Tangent projection.
 *  <p>
 *  The projection is centered at the north pole.
 *  The south pole is projected to the four corners at (+/-1, +/-1).
 *  The equator projects to the diagonals running between the
 *  points (-1,0)->(0,1), (0,1)->(1,0), (1,0)->(0,-1), (-1,0)->(0,-1).
 *  These diagonals divide the four unit squares at the center of the coordinate
 *  grid into 8 right isoceles triangles.
 */  
public class Tot extends skyview.geometry.Projecter {
    
    private static final double sqrt2 = Math.sqrt(2);
    private static Rotater[] inRots = new Rotater[8];
    private static Scaler[]  outScale = new Scaler[8];
    private static Tan       tProj = new Tan();
    
    
    private Straddle myStraddler = new OctaStraddle(2*Math.sqrt(3), this);
    
    public String getName() {
	return "Tot";
    }
    
    public String getDescription() {
	return "Tiled Octahedral Tangent projection";
    }
    private Scaler save1, save2, save0, save0b;

    public boolean isInverse(Transformer obj) {
	return obj instanceof TotDeproj;
    }
    
    public Deprojecter inverse() {
	return new TotDeproj();
    }
    
    private void fillOctant(int octant) {
        double rotate = (octant/2) * Math.PI/2;
        boolean flip  = octant%2 == 1;
        inRots[octant] = new Rotater("zyz", 
                   // Rotate so that the X-axis points to the 45 degree line (or midline of non prime tile) 
                   Math.PI/4 + rotate,
                   // Rotate so that the Z-axis points to the center of the tile
                   // Since we are rotating around the Y axis the Y values won't change.
                   // The 45 degree line will be along the X axis.
                   Math.PI/2-Math.asin(1./Math.sqrt(3)),
                   // Rotate the X axis to Y, so that we have the anticipated geometry
                   Math.PI/2);
        
        if (flip) {
            // Invert the Z axis before rotating.
            double[][] mat = inRots[octant].getMatrix();
            mat[0][2]  *= -1;
            mat[1][2]  *= -1;
            mat[2][2]  *= -1;
        }
        // The maximum Y values should be sqrt(2).
        //  First we need to shift the apex of the triangle back to 0.
        Scaler sc = new Scaler(0, -Math.sqrt(2), 1, 0, 0, 1);
            
        // Now squash the rectangle by a factor of sqrt(2/3) to convert
        // from equilateral to right isoceles.
        sc = sc.add( new Scaler(0.,0., 1,0,0,Math.sqrt(1./3.)));
        
        // The rectangle is now hanging from the origin.  We need to rotate it
        // by 135 degrees to get it to the standard location.
        double isq2 = 1./Math.sqrt(2);
        sc = sc.add(new Scaler(0., 0., -isq2, -isq2, isq2, -isq2));

        // If we are in the southern hemisphere, then flip the the octant
        // along the equator line as a hinge.
        if (flip) {
            double offset = Math.sqrt(3);
            sc = sc.add(new Scaler(offset,offset,0., -1., -1., 0.));
        }
        
        // Finally rotate to the proper location.
        if (rotate != 0) {
            double c = Math.cos(rotate);
            double s = Math.sin(rotate);
            sc = sc.add(new Scaler(0,0, c, -s, s, c));
        }

        outScale[octant] = sc;
            
    }
    
    
    private int getOctant(double x, double y, boolean south) {
        int octant = south? 1:0;
                
        if (x < 0) {
            if (y >= 0) {
                octant += 2;
            } else {
                octant += 4;
            }
        } else if (y < 0) {
            octant += 6;
        }
        return octant;
        
    }
    public void transform(double[] unit, double[] plane) {
	
	
        double x = unit[0];
        double y = unit[1];
        double z = unit[2];
        
        int ind = getOctant(x,y, z<0);
        
        if (inRots[ind] == null) {
            fillOctant(ind);
        }
        double[] xunit = inRots[ind].transform(unit);
        tProj.transform(xunit, plane);
        outScale[ind].transform(plane,plane);
        
        double[] coords = Util.coord(unit);
//        System.err.println("Forward: from: "+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));
//        System.err.println("         to:   "+plane[0]+" "+plane[1]);
               
    }
    
    
    /** Deproject from the plane back to the unit sphere */
    public class TotDeproj extends skyview.geometry.Deprojecter {
	
	public String getName() {
	    return "TotDeproj";
	}
	public String getDescription() {
	    return "Deproject from Tangent Sphere";
	}
	
	public boolean isInverse(Transformer obj) {
	    return obj instanceof Tot;
	}
	
	public Transformer inverse() {
	    return Tot.this;
	}
	
        public void transform(double[] plane, double[] sphere) {
	    
	    double xflip = 1;
	    double yflip = 1;
	    double zflip = 1;
	    
	    double x = plane[0];
	    double y = plane[1];
           
            int ind = getOctant(x,y, Math.abs(x) + Math.abs(y) > Math.sqrt(3));
            
//            System.err.println("Filling out:"+plane[0]+" "+plane[1]+" -> octant:"+ind);
            if (outScale[ind] == null) {
                fillOctant(ind);
            }
            
            try {
            // First scale the image back to the standard Tan projection plane.
                double[] xplane = outScale[ind].inverse().transform(plane);
 //               System.err.println("  Descaling:"+xplane[0]+" "+xplane[1]);
                tProj.inverse().transform(xplane,sphere);
                double[] coords = Util.coord(sphere);
 //               System.err.println("  After deproj:"+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));
                inRots[ind].inverse().transform(sphere, sphere);
                coords = Util.coord(sphere);
 //               System.err.println("  After rot:"+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));
                
            } catch (TransformationException e) {
                sphere[0] = Double.NaN;
                sphere[1] = Double.NaN;
                sphere[2] = Double.NaN;
                return;
            }
            double[] coords = Util.coord(sphere);
 //           System.err.println("  Reverse  from: "+plane[0]+" "+plane[1]);
 //           System.err.println("             to: "+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));

        }	
    }
    
    public static void main(String[] args) throws Exception {
	double x = Math.toRadians(Double.parseDouble(args[0]));
	double y = Math.toRadians(Double.parseDouble(args[1]));
	double[] pos = new double[]{x,y};
        System.err.println("\n\nPos:"+pos[0]+" "+pos[1]);
        double[] unit = Util.unit(pos);
        System.err.println("Unit:"+unit[0]+" "+unit[1]+" "+unit[2]);
        
        double[] out = new double[2];
        double[] vec = new double[3];
        
	Transformer  forward  = new Tot();
	Transformer  back     = forward.inverse();
	
	forward.transform(unit, out);
        System.err.println("Transforming:"+out[0]+" "+out[1]);
	back.transform(out, vec);
	double[] coords = Util.coord(vec);
	
	System.out.println(
            "Input coords:"+args[0]+" "+args[1]+"\n"+                
	    "Original map coordinates: "+pos[0]+" "+pos[1]+"\n"+
	    "Transform to vector:      "+unit[0]+" "+unit[1]+" "+unit[2]+"\n"+
	    "Projects to: "+out[0]+" "+out[1]+"\n"+
	    "Back to unit:  "+vec[0]+" "+vec[1]+" "+vec[2]+"\n"+
            "Which is coords:"+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));            
    }
/*    
    public boolean straddleable() {
	return true;
    }
    
    public boolean straddle(double[][] vertices) {
	return myStraddler.straddle(vertices);
    }
    
    public double[][][] straddleComponents(double[][] vertices) {
	return myStraddler.straddleComponents(vertices);
    }
 */
}
