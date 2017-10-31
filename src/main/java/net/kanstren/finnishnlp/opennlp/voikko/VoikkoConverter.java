package net.kanstren.finnishnlp.opennlp.voikko;

import net.kanstren.finnishnlp.opennlp.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.puimula.libvoikko.Analysis;
import org.puimula.libvoikko.Voikko;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Tokenizes a given document, transforms words into baseforms and keeps track of unidentified words.
 *
 * @author Teemu Kanstren.
 */
public class VoikkoConverter {
	private static final Logger log = LogManager.getLogger();
	/** For finding baseforms and tokenizing words. */
	private static Voikko voikko;
	/** Custom spelling list. Uses previously observed typos or missing word definitions as keys and manually input corrected baseforms as values. */
	private static Properties customSpells = DocUtils.readCustomSpelling();
	/** For tracking and converting words not recognized by Voikko. */
	private final NotFoundWords nfw;

	static {
		//load voikko with the correct dictionary
		try {
			log.info("looking for dict in:" + Config.VOIKKO_DICT_PATH);
			voikko = new Voikko("fi-x-morphoid", Config.VOIKKO_DICT_PATH);
		} catch (Exception e) {
			log.error("Failed to initialize Voikko", e);
			System.exit(1);
		}
	}

	public VoikkoConverter(NotFoundWords nfw) {
		this.nfw = nfw;
	}

	public List<String> basesFor(String word) {
		return basesFor(word, new ArrayList<>());
	}

	public List<String> basesFor2(String word) {
		List<String> strings = basesFor(word, new ArrayList<>());
		if (strings.size() == 0) {
			strings.add(word);
		}
		return strings;
	}

	/**
	 * Get baseforms for given word. A single word can have several baseforms, for example, muutosta=muutto, muutos.
	 * If a word is not recognized by Voikko, custom spell list is used.
	 * If custom spell list has recursive definition, empty list is returned.
	 *
	 * @param word The word to get baseform for.
	 * @return The baseforms for the given word (if found).
	 */
	public List<String> basesFor(String word, Collection<String> usedWords) {
		//skip empty strings which sometimes seems to happen. also avoid infinite loops if word already processed.
		if (word.length() < 1 || usedWords.contains(word)) return Collections.emptyList();

		word = word.toLowerCase();

		List<String> result = new ArrayList<>();

		//analyze gives us the baseforms of the word
		List<Analysis> list = voikko.analyze(word);
		//voikko returns an empty list if it does not recognize the word
		if (list.size() == 0) {
			handleUnrecognizedWord(word, result, usedWords);
			return result;
		}
		//we only come here if the word was recognized by voikko
		for (Analysis analysis : list) {
			//add the baseforms of the found words
			String baseform = analysis.get("BASEFORM").toLowerCase();
			result.add(baseform);
		}
		//this is just to make it alphabetic and deterministic (which it might be anyway)
		Collections.sort(result);
		return result;
	}

	/**
	 * Try to fix a word unrecognized by voikko by looking it up from a list of custom spellings created manually.
	 *
	 * @param word   The unrecognized word.
	 * @param result This is where we store the baseforms or unprocessed words whichever we end up with.
	 */
	private void handleUnrecognizedWord(String word, Collection<String> result, Collection<String> usedWords) {
		if (word.length() < Config.MIN_WORD_LENGTH) {
			return;
		}
		//check the custom spelling list
		String custom = customSpells.getProperty(word);
		//if we found a custom spelling and it is different, we redo this baseform transformation for it. e.g., might be a typo fix for known word.
		if (custom != null && !custom.equals(word)) {
			log.trace("No result from voikko for:" + word + ", found custom spell:" + custom);
			usedWords.add(word);
			result.addAll(basesFor(custom, usedWords));
			return;
		}
		if (custom == null) {
			log.trace("No result from voikko for:" + word + ", adding to list of not found");
			nfw.increment(word);
			return;
		}

		result.add(word);
	}

	public static void main(String[] args) {
		VoikkoConverter vc = new VoikkoConverter(new NotFoundWords());
		Collection<String> bases = vc.basesFor("päivästä");
		System.out.println(bases);
		bases = vc.basesFor("nainen");
		System.out.println(bases);
		bases = vc.basesFor("tietoturvasta");
		System.out.println(bases);
	}
}
