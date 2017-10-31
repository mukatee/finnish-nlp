package net.kanstren.finnishnlp.opennlp.voikko;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Minor utils for processing word strings.
 *
 * @author Teemu Kanstren
 */
public class DocUtils {
  private static final Logger log = LogManager.getLogger();
  /**
   * When persisting the list of unrecognized words, if any of these characters in a word, the word is not written to the list. Modify as needed..
   */
  public static final char[] ignoreList = new char[] {':', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
  /** When cleaning a doc, we remove all these chars. */
  private static char[] badChars = new char[]
      {'(', ')', '.', '?', '”', '”', ':', ';', '/', '\\', '"', '!', '\'', '’', '…', '´', '´', '"', '*', '(', '\'', '•', '+'};


  /**
   * Clean up a given document, removing special characters. For example, unicode dash variants.
   *
   * @param doc The document to clean.
   * @return Same document as given but with special chars (defined as badChars above) removed.
   */
  public static String clean(String doc) {
    StringBuilder sb = new StringBuilder(doc.length());
    char[] chars = doc.toCharArray();
    for (char c : chars) {
      boolean found = false;
      if (c == 0 || (c >= 0xD800 && c <= 0xDFFF)) {
        sb.append(' ');
        continue;
      }

      for (char bc : badChars) {
        if (c == bc) {
          sb.append(' ');
          found = true;
          break;
        }
        //replace weird unicode dash with regular
        if (c == '–') {
          c = '-';
        }
      }
      //this turns uppercase letters to lowercase
//      if (c >= 65 && c <= 90) c -= 32;
      if (!found) sb.append(c);
    }

    String str = sb.toString();
    return str.toLowerCase();
  }

  /**
   * Remove starting and trailing dashes (and whitespace) from a word.
   *
   * @param word The word to trim.
   * @return Trimmed word.
   */
  public static String trimDashes(String word) {
    while (word.startsWith("-")) word = word.substring(1);
    while (word.endsWith("-")) word = word.substring(0, word.length() - 1);
    return word.trim();
  }


  /**
   * Read the list of custom spelling words from the disk.
   *
   * @return The custom baseform transformation mapping.
   */
  public static Properties readCustomSpelling() {
    Properties props = new Properties();
    try {
      FileInputStream stream = new FileInputStream("custom_bases.properties");
      InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
      props.load(isr);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return props;
  }

  /**
   * Check if given string parses as integer.
   *
   * @param text To check.
   * @return True if parses and int.
   */
  public static boolean isInt(String text) {
    try {
      Integer.parseInt(text);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Remove dashes from a word and combine the resulting parts. So "linja-auto" would result as "linjaauto".
   *
   * @param word The string to split and combine.
   * @return The combined word.
   */
  public static String combineDashesFor(String word) {
    String[] split = word.split("-");
    StringBuilder sb = new StringBuilder();
    for (String s : split) {
      sb.append(s);
    }
    return sb.toString();
  }
}

