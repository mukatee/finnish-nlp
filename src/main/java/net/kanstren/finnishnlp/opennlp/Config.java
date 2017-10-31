package net.kanstren.finnishnlp.opennlp;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;

/**
 * Reads configuration from file.
 *
 * @author Teemu Kanstren
 */
public class Config {
	public static final String CONFIG_FILENAME = "finnish-tagger.properties";
	/** Path to Voikko dictionary files. See the README file for more info. */
	public static String VOIKKO_DICT_PATH = "";
	/** Minimum word length to require. */
	public static int MIN_WORD_LENGTH = 3;

	static {
		try {
			Properties props = new Properties();
			FileInputStream stream = new FileInputStream(CONFIG_FILENAME);
			InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
			props.load(isr);

			VOIKKO_DICT_PATH = parseString("voikko.dict.path", props);
			MIN_WORD_LENGTH = parseInt("min.word_length", props, 1, 10);
		} catch (Exception e) {
			System.err.println("Unable to load configuration");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static String parseString(String key, Properties props) {
		String value = props.getProperty(key);
		if (value == null)
			throw new IllegalArgumentException("Missing configuration key: " + key + " in " + CONFIG_FILENAME);
		return value;
	}

	private static boolean parseBoolean(String key, Properties props) {
		String valueStr = props.getProperty(key);
		if (valueStr == null)
			throw new IllegalArgumentException("Missing configuration key: " + key + " in " + CONFIG_FILENAME);
		try {
			return Boolean.parseBoolean(valueStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid boolean value for: " + key + " in " + CONFIG_FILENAME +
				". Was " + valueStr + ", should be formatted as true/false.");
		}
	}

	private static float parseFloat(String key, Properties props) {
		String valueStr = props.getProperty(key);
		if (valueStr == null)
			throw new IllegalArgumentException("Missing configuration key: " + key + " in " + CONFIG_FILENAME);
		float v = 0;
		try {
			v = Float.parseFloat(valueStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid float value for: " + key + " in " + CONFIG_FILENAME +
				". Was " + valueStr + ", should be formatted as valid float (x.y, such as 0.7).");
		}
		return v;
	}

	private static int parseInt(String key, Properties props, int min, int max) {
		String valueStr = props.getProperty(key);
		if (valueStr == null)
			throw new IllegalArgumentException("Missing configuration key: " + key + " in " + CONFIG_FILENAME);
		int v = 0;
		try {
			v = Integer.parseInt(valueStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid integer value for: " + key + " in " + CONFIG_FILENAME +
				". Was " + valueStr + ", should be formatted as valid integer (x.y, such as 10).");
		}
		if (v < min)
			throw new IllegalArgumentException("Value out of bounds: " + key + " in " + CONFIG_FILENAME +
				" was " + v + ", min is " + min);
		if (v > max)
			throw new IllegalArgumentException("Value out of bounds: " + key + " in " + CONFIG_FILENAME +
				" was " + v + ", max is " + max);
		return v;
	}
}
