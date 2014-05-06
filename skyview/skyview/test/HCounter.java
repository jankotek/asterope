package skyview.test;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines a hierarchical counter that is
 * may be used to index a hierarchy of objects (tests, version, ...).
 * The counter is simply a string of integers connected with dots.
 * The user can increment the current level or a higher level in the hierarchy.
 * @author tmcglynn
 */
public class HCounter {

    private ArrayList<Integer> fields = new ArrayList<Integer>();

    /** Create a default counter */
    public HCounter() {
        this(1);
    }

    /** Create a counter starting at a specified integer */
    public HCounter(int init) {
        down(init);
    }
    
    /** Create a counter starting at the specified location */
    public HCounter(int[] init) {
        // If there isn't anything in the array just start a brand new counter.
        if (init == null || init.length == 0) {
            down(1);
        } else {
            for (int i=0; i<init.length; i += 1) {
                down(init[i]);
            }
        }
    }

    /** How many levels in the hierarchy currently in the counter? */
    public int levels() {
        return fields.size();
    }

    /** What is the current value of the counter expressed as a string? */
    public String toString() {
        String val = "";
        String sep = "";
        for (int i: fields) {
            val += sep + i;
            sep = ".";
        }
        return val;
    }

    /** Increment the deepest level of the counter */
    public void increment() {
        increment(levels());
    }

    /** Increment a specific level of the counter.
     *  If this is not the deepest level of the counter
     *  the counter will lose the deeper levels.
     * @param level
     */
    public void increment(int level) {
        if (level > levels()) {
            throw new IllegalArgumentException("Attempt to increment non-existent level");
        }
        fields.set(level-1, fields.get(level-1)+1);
        for (int i=level; i<levels(); i += 1) {
            up();
        }
    }

    /** Add a new level to the counter hierarchy starting at 1. */
    public void down() {
        down(1);
    }

    /** Add a new level to the counter hierarcy starting at a specified value. */
    public void down(int i) {
        fields.add(i);
    }

    /** Discard a level in the hierarchy. */
    public void up() {
        if (levels() < 2) {
            throw new IllegalArgumentException("Attempt to discard top level");
        }
        fields.remove(levels()-1);
        increment();
    }

    /** Is the current value before (or equal to) this input? */
    public boolean before(String input) {
        return compare(input) == -1;
    }

    /** IS the current value after (or equal to) this input? */
    public boolean after(String input) {
        return compare(input) == 1;
    }

    /** Does the given string come before or after the current
     *  value.  The following sequence is increasing:
     *   1 1.1 1.2 1.2.1 1.2.2 1.3.
     *  Returns -1 if the current value comes before the input string,
     *  0 if they are equal or 1 if the current value comes after the input.
     */
    public int compare(String input) {
        if (input == null  || input.length() == 0) {
            throw new IllegalArgumentException("Null comparison");
        }
        String[] tokens = input.split("\\.");

        int cf = Math.min(tokens.length, levels());
        for (int i=0; i<cf; i += 1) {
            try {
                int chk = Integer.parseInt(tokens[i]);
                if (chk < fields.get(i)) {
                    return 1;
                } else if (chk > fields.get(i)) {
                    return -1;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Input "+input+" has non-integer token.");
            }
        }
        // Get here if all strings match.
        if (levels() > tokens.length) {
            // Exhausted tokens but not levels so input comes first.
            return 1;
        } else if (levels() < tokens.length) {
            // Exhausted levels, but not tokens, so input is later is
            // sub-level
            return -1;
        }

        return 0;
    }

    /** Run some simple tests. */
    public static void main(String[] args) {

        HCounter hc = new HCounter();
        System.out.println("Should be 1: "+hc);
        hc.increment();
        System.out.println("Should be 2:"+hc);
        hc.down();
        System.out.println("Should be 2.1: "+hc);
        hc.down(3);
        System.out.println("Should be 2.1.3: "+hc);
        hc.up();
        System.out.println("Should be 2.2: "+hc);
        hc.down();
        System.out.println("Should be 2.2.1: "+hc);
        System.out.println(">"+hc.compare("2.2"));
        System.out.println(">"+hc.compare("2.2.0"));
        System.out.println("="+hc.compare("2.2.1"));
        System.out.println("<"+hc.compare("2.2.1.1"));
        System.out.println("<"+hc.compare("2.3.1"));
        
        hc = new HCounter(3);
        System.out.println("Should be 3:"+hc);
        
        hc = new HCounter(new int[]{4,7,8});
        System.out.println("Should be 4.7.8:"+hc);
        hc.increment();
        System.out.println("Should be 4.7.9:"+hc);
        
    
    }
}
