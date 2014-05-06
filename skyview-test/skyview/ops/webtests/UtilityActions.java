/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package skyview.ops.webtests;

import java.util.Date;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author lmmcdona
 */
public class UtilityActions {

   private int success = 0;
   private int failure = 0;
   private int windowCount = 1;
   private int plotCount = 0;
   private WebDriver driver;
   private JavascriptExecutor jsDriver;
   private List<String> report;
   private Date startTime;
   private String currentTestName;

   UtilityActions(WebDriver driver, List<String> report) {
      this.driver = driver;
      this.report = report;
      this.jsDriver = (JavascriptExecutor) driver;
   }

   void resetWindow() {
      windowCount = 1;
   }

   void incrementWindow() {
      windowCount += 1;
      plotCount = 1;
   }

   void start(String name) {
      startTime = new Date();
      currentTestName = name;
      report.add(name + " started at " + startTime);
   }

   void end() {
      Date endTime = new Date();
      float duration = .001f * (endTime.getTime() - startTime.getTime());
      report.add(currentTestName + " ended at " + endTime + "  Duration: " + duration);
   }

   String spaces(int n) {
      if (n < 10) {
         return "   ";
      } else if (n < 100) {
         return "  ";
      } else if (n < 1000) {
         return " ";
      }
      return "";
   }

   void shutdown() {

      if (failure == 0) {
         driver.close();
      }

      System.out.println("     Report     ");
      for (String line : report) {
         System.out.println(line);
      }

      System.out.println();
      System.out.println("Summary: ");
      System.out.println("   Success:  " + spaces(success) + success);
      System.out.println("   Failure:  " + spaces(failure) + failure);
      System.out.println("   % passed:  " + (100.f * success / (success + failure)));
   }

   void test(String descrip, boolean status) {
      if (status) {
         success += 1;
      } else {
         failure += 1;
         report.add("FAILED: " + currentTestName + ": " + descrip);
      }
   }

   WebElement nextWindow() {
      windowCount += 1;
      String id = "w" + windowCount;
      By bid = By.id(id);

      //WebElement el=driver.findElement(By.id(id));
      WebElement el = driver.findElement(bid);

      return el;
      //return driver.findElement(By.id(id));
   }

   void delay(long millis) {
      try {
         Thread.sleep(millis);
      } catch (Exception e) {
      }
   }

   void setPosition(String pos) {
      WebElement p = driver.findElement(By.id("position"));
      p.clear();
      p.sendKeys(pos);
   }

   WebElement clickID(String id) {
      WebElement res = driver.findElement(By.id(id));
      res.click();
      return res;
   }

   WebElement getParent(WebElement entry) {
      return getParent(entry, 1);
   }

   WebElement getParent(WebElement entry, int n) {
      for (int i = 0; i < n; i += 1) {
         entry = entry.findElement(By.xpath(".."));
      }
      return entry;
   }

   void addResult(boolean succeeded) {
      if (succeeded) {
         success += 1;
      } else {
         failure += 1;
      }
   }

   Object executeScript(String script) {
      return jsDriver.executeScript(script);
   }

   //-- Grab file name from URL
   String urlBasename(String url) {


      String baseName = FilenameUtils.getBaseName(url);
      String extension = FilenameUtils.getExtension(url);

      //System.out.println("Basename : " + baseName);
      //System.out.println("extension : " + extension);
      return baseName + "." + extension;
   }
}
