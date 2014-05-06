package skyview.util;

import skyview.executive.Settings;

/**
 * Used to force a process to timeout after a period of time
 *
 * @author lmmcdona
 */
public class SetTimeout {
   //--- Start separate thread to wait 1 hour and then exit
   //--- Should take care of non-responding remote survey queries

   private static long millis;
   private static String[] skeys = {"siabase", "siaimagetimeout", "siapcoordinates",
      "siapfilterfield", "siapfiltervalue", "siapnaxis", "siapprojection", "siapurl",
      "size", "survey", "coordinates", "imagesize", "output", "pixels", "position", "scale"};

   public void set(long time) {
      millis = time;
      runThread();

   }

   public static void set() {
      millis = 3600000;

      runThread();
   }

   public static void runThread() {
      new Thread(new Runnable() {

         /*
          * @Override public void run() { throw new
          * UnsupportedOperationException("Not supported yet."); }
          */
         @Override
         public void run() {
            try {
               System.err.println("Starting CGI timer " + millis);
               Thread.sleep(millis);

            } catch (InterruptedException e) {
            };
            System.err.println("EXITING after timeout of " + millis + " milliseconds");
            System.err.println("Query Settings");
            System.err.println("--------------");
            String settings = "";


            java.util.Arrays.sort(skeys);
            for (String key : skeys) {

               if (key == null || key.length() == 0 || key.charAt(0) == '_') {
                  continue;  // Skip internal communication settings.
               }

               String val = Settings.get(key);
               if (val == null) {
                  settings += key + " is null\n";
               } else {
                  settings += key + " --> " + val + "\n";
               }
            }
            System.err.println(settings);
            System.err.println("--------------");

            System.exit(1);
         }
      }).start();

   }
}