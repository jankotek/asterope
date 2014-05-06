/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nom.tam.image;

import java.io.IOException;

/**
 *
 * @author tmcglynn
 */
public interface ImageTiler {

    public Object getTile(int[] corners, int[] lengths) throws IOException;
    public void getTile(Object array, int[] corners, int[] lengths) throws IOException;
    public Object getCompleteImage() throws IOException;
    
}
