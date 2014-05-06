

package skyview.tile;

import java.util.List;
import skyview.executive.Settings;
import skyview.survey.Image;

/**
 * This interface defines methods for generating
 * a set of tiles over a region of the sky.
 * 
 * @author tmcglynn
 */
public interface Tiler {
    
    public abstract void initialize();
    public abstract int getImageCount();
    public abstract List<double[]> getCenters();
    public abstract Image getImage(int i);

}
