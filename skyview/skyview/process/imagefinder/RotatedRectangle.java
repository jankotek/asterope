

package skyview.process.imagefinder;

import skyview.survey.Image;
import skyview.geometry.Scaler;

import java.util.Map;
import java.util.HashMap;
import skyview.executive.Settings;
import skyview.survey.FitsImage;


/**
 * This image finder uses the same algorithm as the border finder, but rather than
 * using the rectangle in the projection plane, we assume that only a subrectangle
 * which of the image (which may be rotated with respect to the image) is valid.
 * 
 * A limits file is associated with each input image and contains a specification of the
 * bounding rectangle in terms of the pixels of the original image.
 * 
 * @author tmcglynn
 */
public class RotatedRectangle extends Border {
    
    /** This class saves the information for a given image. */
    private class InnerRectInfo {
        int h;
        int w;
        Scaler rot;  
        Image base;
    };
    
    private InnerRectInfo curr;
    private java.util.Map<Image,InnerRectInfo> rotInfos = new HashMap<Image, InnerRectInfo>();

    
    public int[] getInputLimits(Image in) {
        validate(in);
        return new int[]{curr.w, curr.h};
    }
    
    public double[] getImage(Image in, double[] vector) {
        validate(in);
        double[] out = super.getImage(in, vector);
        
        
        double[] out2 = curr.rot.transform(out);
        return out2;
    }
    
    private void validate(Image in) {
        if (curr != null && curr.base == in) {
            return;
        }
        curr = rotInfos.get(in);
        if (curr == null) {
            curr = downloadLimits(in);
            rotInfos.put(in, curr);
        }
    }
    
    private InnerRectInfo downloadLimits(Image in) {
        if (! (in instanceof FitsImage)) {
            throw new IllegalArgumentException("External limits only available for local data");
        }
        String name = ((FitsImage) in).getFilename()+".limits";
        Settings.updateFromFile(name);
        InnerRectInfo curr = new InnerRectInfo();
        
        double wr = Double.parseDouble(Settings.get("rect.width"));
        double hr = Double.parseDouble(Settings.get("rect.height"));
        double x0 = Double.parseDouble(Settings.get("rect.x0"));
        double y0 = Double.parseDouble(Settings.get("rect.y0"));
        double ar = Math.toRadians(Double.parseDouble(Settings.get("rect.angle")));
        curr.w = (int)Math.ceil(wr);
        curr.h = (int)Math.ceil(hr);
        curr.base = in;
        
        // Create a scaler in three parts.
        //  ... Shift to the center of the apparent image.
        Scaler sc = new Scaler(-x0, -y0, 1, 0, 0, 1);
        
        //  ... Rotate so that the filled region is aligned with the axes
        sc = sc.add(new Scaler(0, 0, Math.cos(ar), Math.sin(ar), -Math.sin(ar), Math.cos(ar) ));
        
        //  ... Now shift so that the center is at the corner of the filled region.
        sc = sc.add(new Scaler(wr/2, hr/2, 1, 0, 0, 1));
        curr.rot = sc;
        return curr;        
    }
}
