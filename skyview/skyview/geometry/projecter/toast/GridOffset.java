

package skyview.geometry.projecter.toast;

/**
 * 
 * @author tmcglynn
 */
public class GridOffset {
    /*
 * Consider a pixel on a tile T_uv at level l which has s levels of subdivisions.
 * This contains pixels with n_min <= l+s.
 * 
 * Consider another tile T_ab at level c with d levels of subdivisions where we
 * have  c >= l, i.e., this is a finer level tile.
 * 
 * Does the first tile contain the second.
 * 
 * Let r be the ratio of resolutions of the tiles, i.e., r=2^(c-l).  Then
 * the first tile contains the second if
 *    u/r = a, v/r=b
 * 
 * Now consider the points in the second tile.  What points do they correspond to
 * in the first tile.  If we have a point g,h in the second tile then
 * we know that the scales are related with delta x ~ delta g *2^(l+s-c-d).  If g=0 then
 * we must be at the beginning of the second tile and we have
 *     x = (a%r)/r * 2^s  so generally
 * 
 *     x = (a%r)/r * 2^s + g * 2^(l+s-c-d)
 *     y = (b%r)/r * 2^s + h * 2^(l+s-c-d)
 * 
 * or
 *    g = [ x - a%r/r * 2^s] * 2^(c+d-l-s)
 *    h = [ y - b%r/r * 2^s] * 2^(c+d-l-s)
 * 
 * What is the index in the second tile.
 * 
 * The 0,0 point of the 
  */
    
    public static void main(String[] args) {
        
        int l0 = Integer.parseInt(args[0]);
        int nx0 = Integer.parseInt(args[1]);
        int ny0 = Integer.parseInt(args[2]);
        int s0 = Integer.parseInt(args[3]);
        int nx = Integer.parseInt(args[4]);
        int ny = Integer.parseInt(args[5]);
        
        int l1 = Integer.parseInt(args[6]);
        int s1 = Integer.parseInt(args[7]);
        
        
        System.out.println("Starting at tile "+nx0+","+ny0+" at level "+l0 +" with sublevels:"+s0);
        System.out.println("Using pixel:"+nx+","+ny);
        System.out.println("Moving up to max level tile at level "+l1+" with tile sublevels:"+s1);
        
        double tx  = 1.*nx0/Math.pow(2,l0) + nx/Math.pow(2,l0+s0);
        double ty  = 1.*ny0/Math.pow(2,l0) + ny/Math.pow(2,l0+s0);
        
        for (int l = l0+1; l<=l1; l += 1) {
                    
        
        
            double delta = Math.pow(2,l);
            int nx1 = (int) (delta*tx);
            int ny1 = (int) (delta*ty);
        
       
            double gx = (tx-1*nx1/Math.pow(2,l))*Math.pow(2, l+s1);
            double gy = (ty-1*ny1/Math.pow(2,l))*Math.pow(2, l+s1); 
            if (
                    Math.abs(gx - Math.round(gx)) < 1.e-8 &&
                    Math.abs(gy - Math.round(gy)) < 1.e-8) {
                System.out.println("Grid: "+l+" "+nx1+" "+ny1+" "+s1+" -> pixel:"+gx+" "+gy);
            }
        }        
    }
}