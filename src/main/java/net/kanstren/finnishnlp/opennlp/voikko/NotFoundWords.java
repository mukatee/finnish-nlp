package net.kanstren.finnishnlp.opennlp.voikko;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import osmo.common.TestUtils;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Tracks words that have not been recognized by Voikko.
 * Words are store in a properties file where the property key is the word string and value is how many times it has been seen.
 *
 * @author Teemu Kanstren
 */
public class NotFoundWords {
	private static final Logger log = LogManager.getLogger();
	/** Key = word, count = how many times seen. */
	private Map<String, WordCount> counts = new HashMap<>();
	/** Name of file for persistent storage of the words. */
	private static final String filename = "not-found-words.properties";

	public NotFoundWords() {
		try {
			Properties props = new Properties();
			FileInputStream stream = new FileInputStream(filename);
			InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
			props.load(isr);
			initFrom(props);
		} catch (Exception e) {
			log.trace("Unable to load "+filename, e);
		}
	}

	public NotFoundWords(Properties props) {
		initFrom(props);
	}

	private void initFrom(Properties props) {
		Set<String> names = props.stringPropertyNames();
		for (String name : names) {
			String countStr = props.getProperty(name);
			int count = Integer.parseInt(countStr);
			counts.put(name, new WordCount(name, count));
		}
	}

	/**
	 * Increment the count of how many times a word has been seen.
	 *
	 * @param word The word to increment.
	 */
	public synchronized void increment(String word) {
		WordCount wordCount = counts.computeIfAbsent(word, w -> new WordCount(w, 0));
		wordCount.count++;
	}

	/**
	 * Sort the words by count (most common first) and write them to file.
	 */
	public synchronized void save() {
		String sorted = sortedProperties();
		TestUtils.write(sorted, filename);
		log.info("Wrote " + counts.size() + " unknown words.");
	}

	public synchronized void removeAll(Collection<String> words) {
		counts.keySet().removeAll(words);
	}

	/**
	 * Sort the words that have not been seen by their count (most common first) and build a properties string for them. For storage etc.
	 *
	 * @return Properties format sorted string.
	 */
	public synchronized String sortedProperties() {
		List<WordCount> sorted = new ArrayList<>(counts.size());
		StringBuilder sb = new StringBuilder(sorted.size() * 20);
		sorted.addAll(counts.values());
		Collections.sort(sorted);
		for (WordCount wordCount : sorted) {
			//skip short words, many typos, shortened versions, etc.
			if (wordCount.word.length() < 3) continue;
			boolean ok = true;
			for (char c : DocUtils.ignoreList) {
				if (wordCount.word.indexOf(c) >= 0) {
					//any of the chars in the "ignoreList" cause the word to be ignored and not written to disk
					ok = false;
					break;
				}
			}
			if (!ok) continue;
			sb.append(wordCount.word);
			sb.append("=");
			sb.append(wordCount.count);
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	/**
	 * Keep track of times a word has been seen, and enable sorting by count.
	 */
	private static class WordCount implements Comparable<WordCount> {
		public int count;
		public final String word;

		public WordCount(String word, int count) {
			this.word = word;
			this.count = count;
		}

		@Override
		public int compareTo(WordCount o) {
			return o.count - count;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			WordCount wordCount = (WordCount) o;

			return word.equals(wordCount.word);

		}

		@Override
		public int hashCode() {
			return word.hashCode();
		}
	}
}
