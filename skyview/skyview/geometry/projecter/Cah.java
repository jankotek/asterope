package skyview.geometry.projecter;

import skyview.executive.Settings;
import skyview.geometry.Converter;
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
public class Cah extends skyview.geometry.Projecter {
    
    private static final double sqrt2 = Math.sqrt(2);
    private static Rotater[] inRots = new Rotater[8];
    private static Scaler[]  outScale = new Scaler[8];
    private static Tan       tProj = new Tan();
    
    private Scaler sv1, sv2, sv3a, sv3b, sv3;
    
    
    public String getName() {
	return "Cah";
    }
    
    public String getDescription() {
	return "Cahill octahedral projection";
    }

    public boolean isInverse(Transformer obj) {
	return obj instanceof CahDeproj;
    }
    
    public Deprojecter inverse() {
	return new CahDeproj();
    }
    
    private double a = Math.sqrt(6);
    private double s3d2 = Math.sqrt(3)/2;
    
//    private double[][] offsets = 
//      {  {0, 0.5*a},  {s3d2*a, -a},      {0, 0.5*a}, {2*s3d2*a, 0.5*a},
//         {0, 0.5*a},  {-2*s3d2*a, 0.5*a},{0, 0.5*a}, {-s3d2*a, -a} };
    
    private double[] angles  = {
       30, 30, 90, 90, 270, 270, 330,330
    };
    
    private void fillOctant(int octant) {
        
        double rotate = (octant/2) * Math.PI/2;
        boolean flip  = octant % 2 == 1;
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
        // After this transformation we have an equilateral triangle in -y with
        // the apex at the origin.
        Scaler sc = new Scaler(0, -Math.sqrt(2), 1, 0, 0, 1);
        sv1 = sc;
        
        if (flip) {
            // For the southern hemisphere, we first flip the projection in y
            // giving us a equilateral triangle in +y teetering at the origin.
            sv3a = new Scaler(0,0, 1, 0, 0, -1);
            sc = sc.add(sv3a);
            // Then we shift it down by the twice diagonal length to
            // place it directly below the northern wedge, i.e., the
            // north and south wedges together would look like a vertical
            // diamond.
            sv3b = new Scaler(0, -2*s3d2*a, 1, 0, 0., 1);
            sc = sc.add(sv3b);
        }
        double cs = Math.cos(Math.toRadians(angles[octant]));
        double sn = Math.sin(Math.toRadians(angles[octant]));

        // Now rotate the image into the correct orientation for the
        // given longitude.
        sv2 = new Scaler(0, 0, cs, -sn, sn, cs);
        sc = sc.add(sv2);
        
        // Finally shift everything up a bit so that the center of the image
        // is at 0,45 rather than the pole.
        sv3 = new Scaler(0, a/2, 1, 0, 0, 1);
        sc = sc.add(sv3);
        outScale[octant] = sc;            
    }
    
    /** For the Cahill projection this works only for
     *  the coordinates, not in the projection plane.
     */
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
        double[] xp = plane.clone();
        outScale[ind].transform(plane,plane);
/*        
        System.out.println("Scale by steps0:"+xp[0]+" "+xp[1]);
        sv1.transform(xp,xp);
        System.out.println("Scale by steps1:"+xp[0]+" "+xp[1]);
        if (ind % 2 == 1) {
            sv3a.transform(xp,xp);
            System.out.println("Scale by steps3a:"+xp[0]+" "+xp[1]);
            sv3b.transform(xp,xp);
            System.out.println("Scale by steps3b:"+xp[0]+" "+xp[1]);
        }
        sv2.transform(xp,xp);
        System.out.println("Scale by steps2:"+xp[0]+" "+xp[1]);
        sv3.transform(xp,xp);
        System.out.println("Scale by steps3:"+xp[0]+" "+xp[1]);
  */      
               
    }
    
    
    /** Deproject from the plane back to the unit sphere */
    public class CahDeproj extends skyview.geometry.Deprojecter {
	
	public String getName() {
	    return "CahDeproj";
	}
	public String getDescription() {
	    return "Deproject from Tangent Sphere";
	}
	
	public boolean isInverse(Transformer obj) {
	    return obj instanceof Cah;
	}
	
	public Transformer inverse() {
	    return Cah.this;
	}
        
        int[][][] octants = 
        {
            { 
                {-1,-1}, {-1, 7}, {-1, 1}, {-1,-1}
            },
            {
                {-1,-1}, {7, 6}, {1,0}, {-1,-1}
            },
            { 
                {-1,5}, {6,4}, {0,2}, {-1,3}                 
            },
            {
                {5,-1}, {4,-1}, {2,-1}, {3,-1}
            }
        };
        private int getOctant(double x, double y) {
            
            double tx = s3d2*a;
            double ty = 0.5*a;
            // Shift to make corner of bounding box, the origin
            
            x += 2*s3d2*a;
            y += a;
            
            // Convert to 'pixels' that break up map into rectangular
            // sections that we can use to find the appropriate octant.
            x /= tx;
            y /= ty;
            
            int ix = (int) x;
            int iy = (int) y;
            
            // Outside the bounding box.
            if (ix < 0 || ix > 3  || iy < 0 || iy > 3) {
                return -1;
            }
            
            double dx = x - ix;
            double dy = y - iy;
            
            // Within each of the rectangular boxes we have
            // a different facet depending upon whether we
            // are above or below a diagonal in the box.  But
            // which diagonal.  For the diagonal in the bottom left
            // corner it is a rising (dexter I think) diagonal.
            // It alternates like a checker board.            
            boolean rise = (ix + 5*iy)%2 == 1;
            
            int sub = 0;
            if (rise) {
                if (dy > dx) {
                    sub = 1;
                }
            } else {
                if (dy > 1-dx) {
                    sub = 1;
                }
            }
            return octants[iy][ix][sub];            
        }
	
        public void transform(double[] plane, double[] sphere) {
	    
	    double xflip = 1;
	    double yflip = 1;
	    double zflip = 1;
	    
	    double x = plane[0];
	    double y = plane[1];
           
            int ind = getOctant(x,y);
            
            
            if (ind < 0) {
                sphere[2] = Double.NaN;
                sphere[0] = Double.NaN;
                sphere[1] = Double.NaN;
                return;
            }
            
            if (outScale[ind] == null) {
                fillOctant(ind);
            }
            
            try {
            // First scale the image back to the standard Tan projection plane.
                double[] xplane = outScale[ind].inverse().transform(plane);
                tProj.inverse().transform(xplane,sphere);
                double[] coords = Util.coord(sphere);
                inRots[ind].inverse().transform(sphere, sphere);
                coords = Util.coord(sphere);
                
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
        
	Transformer  forward  = new Cah();
	Transformer  back     = forward.inverse();
	
        
	forward.transform(unit, out);
	back.transform(out, vec);
	double[] coords = Util.coord(vec);
	
	System.out.println(
            "Input coords:             "+args[0]+" "+args[1]+"\n"+                
	    " (in radians):            "+pos[0]+" "+pos[1]+"\n"+
	    " (as unit vector):        "+unit[0]+" "+unit[1]+" "+unit[2]+"\n"+
	    "Projects to:              "+out[0]+" "+out[1]+"\n"+
	    "Which deprojects to :     "+vec[0]+" "+vec[1]+" "+vec[2]+"\n"+
            " (as coords):             "+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));            
        
        System.out.println("\n\nLooking at reverse directly");
        out[0] = Double.parseDouble(args[0]);
        out[1] = Double.parseDouble(args[1]);
        back.transform(out,vec);
        coords = Util.coord(vec);
        System.out.println(
                "Input position:   "+out[0]+" "+out[1] + "\n"+
                "Goes to vector:   "+vec[0]+" "+vec[1] + vec[2]+"\n"+
                "  Coords:     :   "+Math.toDegrees(coords[0])+" "+Math.toDegrees(coords[1]));
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
