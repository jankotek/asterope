/**
 *
 * @author lmmcdona
 */
package skyview.ops.webtests;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author lmmcdona
 */
public class SkyViewTests {

   String[] tests = {
      "SimpleImageQuery", //  0
      "CatalogQuery", //  1
      "CheckInteraction" //  2
   };
   //   private WebDriver driver;
   static WebDriver driver;
   static String dlDir;
   private List<String> report;
   private UtilityActions ua;
   String baseURL = "http://skyview.gsfc.nasa.gov/current/cgi/query.pl";
   String browser = "Firefox";
   private int minTest = 0;
   private int maxTest = tests.length-1;

   ////////////   Methods in this section control the overall testing behavior
   // Usage: XaminTests [browser [minTest [maxTest]]]
   public static void main(String[] args) {
      new SkyViewTests().run(args);
   }

   void run(String[] args) {
      Map<String, String> env = System.getenv();
      for (String envName : env.keySet()) {
         //System.out.format("%s=%s%n", envName, env.get(envName));
         if (envName.equals("DOWNLOADDIR")) {
            dlDir = env.get(envName);
            //System.err.println(envName + " -->" + env.get(envName) + " -->" + dlDir);
         }

      }

      //System.err.println("Number of args: " + args.length);
      if (args.length == 0) {
         Scanner sc = new Scanner(System.in);

         //System.out.println("Usage: ");
         System.out.println("\nUsing default arguments: " + browser + "  " + baseURL + "  " + minTest + " " + maxTest);

         System.out.println("Continue? (y/n): ");
         //prompt user to continue 
         String userinput = sc.next();

         if (!userinput.equalsIgnoreCase("y")) {
            System.exit(0);
         }
      }
      if (args.length > 0) {  //browser
         browser = args[0];
      }
      if (args.length > 1) {
         baseURL = args[1];
      }
      if (args.length > 2) {   // start test
         minTest = Integer.parseInt(args[2]);
      }
      if (args.length > 3) {    // end test
         maxTest = Integer.parseInt(args[3]);
      } else if (args.length == 3) {
         maxTest = minTest + 1;  // Do a single Test;
      }
      System.out.println("\nTesting " + baseURL + " using browser " + browser 
              + ":: Tests: " + minTest +"-" + maxTest);

      try {

         setup();

         if (minTest < 0) {
            minTest = 0;
         }
         if (maxTest > tests.length) {
            maxTest = tests.length;
         }
         for (int i = minTest; i < maxTest; i += 1) {
            String method = tests[i];
            try {
               java.lang.reflect.Method m = this.getClass().getDeclaredMethod(method, new Class[0]);
               ua.start(method);
               m.invoke(this);
               // Add one for the test as a whole (versus the catch block)
               ua.end();
               ua.addResult(true);
            } catch (Exception e) {
               report.add(method + " exception:" + e);
               ua.addResult(false);

               if (e.getCause() != null) {
                  System.err.println("*** Exception in test " + i + " ***");
                  System.err.println("Exception traceback:");
                  e.getCause().printStackTrace(System.err);
               }
            }
         }
      } finally {
         ua.shutdown();
      }
   }

   void setup() {
      //System.err.println("Setting up!!!");
      try {
         //System.err.println("browser=" + browser);

         report = new ArrayList<String>();
         // Latest version of Firefox won't work
         if (System.getProperty("webdriver.firefox.bin") == null) {
            if (new java.io.File("/usr1/local/bin/firefox").exists()) {
               System.setProperty("webdriver.firefox.bin", "/usr1/local/bin/firefox");
            } else if (browser.equalsIgnoreCase("firefox")
                    && new java.io.File("/Volumes/Apps_Docs/Programs/Firefox.app/Contents/MacOS/firefox").exists()) {
               System.setProperty("webdriver.firefox.bin", "/Volumes/Apps_and_Docs/Programs/Firefox.app/Contents/MacOS/firefox-bin");
            }
         }

         if (System.getProperty("webdriver.chrome.bin") == null) {
            if (browser.equals("chrome")
                    && new java.io.File("/Volumes/ASD_Snow_Leopard_System/Applications/Google Chrome.app/Contents/MacOS/Google Chrome").exists()) {
               System.err.println("chrome defined");
               System.setProperty("webdriver.chrome.bin", "/Volumes/ASD_Snow_Leopard_System/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            }
         }

         browser = browser.toLowerCase();
         //System.err.println(browser);
         if (browser.equals("firefox")) {
            //System.err.println("Driver:" + System.getProperty("webdriver.firefox.bin"));
            //--- Set preference to save downloads without prompt
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.manager.showWhenStarting", false);

            //set folder path where tar file is saved
            String os = System.getProperty("os.name");


            if (dlDir == null && os.startsWith("Mac ")
                    && new File("/Volumes/Apps_Docs/lmmcdona/Downloads").exists()) {
               dlDir = "/Volumes/Apps_Docs/lmmcdona/Downloads";
               System.err.println("dlDir set!");
            } else {
               boolean dlfound = true;
               if (dlDir == null) {
                  System.out.println("\n---------\nA download directory has not been specified\n"
                          + "in environment variable DOWNLOADDIR\n");
                  dlfound = false;
               } else if (!new File(dlDir).exists()) {
                  System.out.println("\n---------\nThe download directory "
                          + dlDir + " specified\n"
                          + "in environment variable DOWNLOADDIR does not exist.\n");
                  dlfound = false;

               }

               if (!dlfound) {
                  Scanner sc = new Scanner(System.in);
                  // create new scanner

                  System.out.println("This variable is needed for the image file tests.\n");
                  //System.out.println("All other tests will run without the variable set.");
                  System.out.println("---------\n");
                  //System.out.println("Continue? (y/n): ");
                  //prompt user to continue 
                  //String userinput = sc.next();

                  //if (!userinput.equalsIgnoreCase("y")) {
                  System.exit(0);
                  //}
               } else {
                  System.err.println("\nDownload directory=" + dlDir);
                  profile.setPreference("browser.download.dir", dlDir);
               }
            }


            profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                    "application/x-tar,application/x-gtar");
            driver = new FirefoxDriver(profile);

         } else if (browser.equals("html")) {
            driver = new HtmlUnitDriver(true);

         } else if (browser.equals("ie")) {
            driver = new InternetExplorerDriver();

         } else if (browser.equals("chrome")) {
            System.setProperty("webdriver.chrome.driver", "/Volumes/Apps_Docs/lmmcdona/Work/drivers/chromedriver");
            driver = new ChromeDriver();

         }

         driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
         ua = new UtilityActions(driver, report);

         //baseURL = "http://skyview.gsfc.nasa.gov/skyl/cgi/query.pl";
         //if (System.getProperty("xamin.url") != null) {
         //baseURL = System.getProperty("xamin.url");
         //}

         //System.out.println("Starting at base URL: " + baseURL);
         //System.out.println("Using driver for: " + browser);

         driver.get(baseURL);
         report.add("Setup complete at:" + new Date());


      } catch (Exception e) {
         System.err.println("Error during setup:" + e);
         e.printStackTrace(System.err);
      }
   }

   /**
    * A discovery query of all HEASARC tables containing 3c273.
    */
   void SimpleImageQuery() {
      String winHandleBefore = driver.getWindowHandle();

      WebElement element = driver.findElement(By.id("object"));
      if (element == null) {
         System.err.println("no object element");

      }
      element.clear();
      element.sendKeys("3c273");
      WebElement dssoption = null;
      List<WebElement> olist = driver.findElements(By.tagName("option"));
      for (WebElement o : olist) {
         String txt = o.getText();
         if (txt.equals("DSS")) {
            dssoption = o;

            break;
         }
      }

      if (dssoption != null) {
         dssoption.click();
         dssoption.submit();

      } else {
         System.err.println("no survey element selec");

      }   //


      //Switch to new window opened
      int count = 0;
      for (String winHandle : driver.getWindowHandles()) {
         count++;
         driver.switchTo().window(winHandle);
         //System.err.println("handle count: " + count);
      }


      ua.delay(1000);

      List<WebElement> list = driver.findElements(By.linkText("FITS"));

      //System.err.println("links:" + list.size());
      WebElement link = list.get(0);
      String href = link.getAttribute("href");
      String filename = ua.urlBasename(href);
      //System.err.println("href=" + href + "-->" + filename);


      link.click();
      ua.delay(8000);

      //--- 
      //System.err.println("looking for " + dlDir + "/" + filename);

      File fitsFile = new File(dlDir + "/" + filename);
      //--- Check if download file exists
      boolean file_exists = fitsFile.exists();


      long size = fitsFile.length();
      System.err.println("------------------------------------------");
      System.err.println("Existence of downloaded FITS file" + ": "
              + file_exists + " wanted: " + true);
      ua.test("Existence of downloaded FITS file", file_exists == true);
      if (file_exists) {
         ua.test("Size of downloaded FITS file", size > 20000);
         System.err.println("Size of downloaded FITS file: " + size
                 + " wanted more than "
                 + " " + 20000);
         System.err.println("------------------------------------------\n\n");
         //--- remove FITS file after test
         boolean deleted = fitsFile.delete();
         if (deleted) {
            //System.err.println("FITS file has been deleted");
         } else {
            System.err.println("Unable to delete FITS file");
         }

      }



      //Close the new window, if that window no more required
      driver.close();

      //Switch back to original browser (first window)
      driver.switchTo().window(winHandleBefore);

      /*
       * ua.test("Number of of elements", (list.size() > 150));
       *
       * int tabsToTest = 4; for (int line = 0; line < tabsToTest; line += 1) {
       *
       * WebElement row = list.get(line); List<WebElement> cols =
       * row.findElements(By.cssSelector("div.x-grid3-cell-inner")); String
       * table = cols.get(0).getText().trim(); String count =
       * cols.get(1).getText(); int cnt = Integer.parseInt(count); row.click();
       * element = ua.nextWindow();
       *
       * List<WebElement> queryList = ua.getGridRows(element); ua.test("Grid
       * table query:" + table, queryList.size() == cnt); WebElement col =
       * cols.get(0); WebElement img = col.findElement(By.xpath(".//img[2]"));
       * String val = img.getAttribute("src"); img.click(); List<String>
       * selectedTables = (List<String>) ua.executeScript("return
       * xamin.tree.getQueryTables();"); ua.test("Pushed table size:" + table,
       * selectedTables.size() == 1); ua.test("Pushed table id:" + table,
       * selectedTables.get(0).trim().equals(table)); }
       *
       * ua.cleanQueries(); ua.clearAll();
       */
   }

   void CatalogQuery() {
      //System.err.println("In catalog query");
      String winHandleBefore = driver.getWindowHandle();
      List<WebElement> inputs = driver.findElements(By.tagName("input"));
      for (WebElement i : inputs) {
         String value = i.getAttribute("value");
         if (value.equals("Reset")) {
            //System.err.println("Resetting form");
            i.click();
            break;
         }
      }
      WebElement element = driver.findElement(By.id("object"));

      //if empty enter a source name
      if (element != null) {
         element.clear();
         element.sendKeys("3c273");
         if (element == null) {
            System.err.println("Coordinates/Source box is empty");
            return;

         }
      }
      Select surveyselect = new Select(driver.findElement(By.id("GammaRay")));
      if (surveyselect == null) {
         System.err.println("Survey was not selected");
         return;
      }

      surveyselect.selectByVisibleText("Fermi 5");


      WebElement overlay_exp;
      overlay_exp = driver.findElement(By.name("overlays"));
      if (overlay_exp == null) {
         System.err.println("Cannot expand overlays section");
         return;
      }
      overlay_exp.click();
      ua.delay(600);
      Select catselect = new Select(driver.findElement(By.id("cataloglist")));

      if (catselect == null) {
         System.err.println("Catalog list was not found");
         return;
      }


      catselect.selectByIndex(0);
      //System.err.println("catalog selected ");

      ua.delay(1000);
      element.submit();



      //Switch to new window opened
      for (String winHandle : driver.getWindowHandles()) {
         driver.switchTo().window(winHandle);
      }


      ua.delay(1000);

      List<WebElement> list = driver.findElements(By.linkText("Data"));

      //System.err.println("links:" + list.size());
      WebElement link = list.get(0);
      String href = link.getAttribute("href");
      String filename = ua.urlBasename(href);
      //System.err.println("href=" + href + "-->" + filename);


      link.click();
      ua.delay(8000);

      //---  selected row
      //System.err.println("looking for " + dlDir + "/" + filename);

      File fitsFile = new File(dlDir + "/" + filename);
      //--- Check if download file exists
      boolean file_exists = fitsFile.exists();


      long size = fitsFile.length();
      System.err.println("------------------------------------------");
      System.err.println("Existence of downloaded catalog list file" + ": "
              + file_exists + " wanted: " + true);
      ua.test("Existence of downloaded catalog list file", file_exists == true);
      if (file_exists) {
         ua.test("Size of downloaded catalog list file", size > 900);
         System.err.println("Size of downloaded catalog list file: " + size
                 + " wanted more than "
                 + " " + 900);
         System.err.println("------------------------------------------\n\n");

         //--- remove  file after test
         boolean deleted = fitsFile.delete();
         if (deleted) {
            //System.err.println("Catalog list file has been deleted\n");
         } else {
            System.err.println("Unable to delete catalog list file\n");
         }
      }




   }

   void checkInteraction() {
      System.err.println("In check Interaction");
      String winHandleBefore = driver.getWindowHandle();
      List<WebElement> links = driver.findElements(By.tagName("a"));
      for (WebElement i : links) {
         String value = i.getAttribute("name");
         if (value.equals("otheropts")) {

            System.err.println("expanding other options");

            i.click();
            break;
         }
      }
      WebElement expanded = driver.findElement(By.id("optparams"));

      if (expanded == null) {
         System.err.println("Error: Expanded options not found");
         //return;
      } else {
         //System.err.println( " displayed " + expanded instanceof RenderedWebElement);
         System.err.println("after expansion" + expanded.isDisplayed());
      }
   }
}
