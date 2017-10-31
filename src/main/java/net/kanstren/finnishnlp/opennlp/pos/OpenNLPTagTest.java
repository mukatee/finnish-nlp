package net.kanstren.finnishnlp.opennlp.pos;

import net.kanstren.finnishnlp.opennlp.voikko.NotFoundWords;
import net.kanstren.finnishnlp.opennlp.voikko.VoikkoConverter;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Teemu Kanstren
 */
public class OpenNLPTagTest {
	private static final Logger log = LogManager.getLogger();
	private static int loadedWords = 0;
	private static int loadedSentences = 0;
	private static int correct = 0;
	private static int guesses = 0;
	private static int unknowns = 0;
	private static Map<String, Integer> unknownTagFreqs = new HashMap<>();
	private static long totalTagTime = 0;
	private static POSTaggerME tagger = null;
	//if true, we are just collecting treebank words that voikko does not recognize and not tagging anything
	private static boolean collectingUnknowns = true;
	private static NotFoundWords nfw = new NotFoundWords();
	private static VoikkoConverter vc = new VoikkoConverter(nfw);

	public static void main(String[] args) throws Exception {
		log.info("Starting ONLP prediction tester");
		String modelFilename = args[0];
		log.info("Loading model '" + modelFilename + "'.");
		InputStream modelIn = new FileInputStream(modelFilename);
		POSModel model = new POSModel(modelIn);
		tagger = new POSTaggerME(model);
		log.info("Model loaded.");

		String sentenceFilename = args[1];
		log.info("Starting to test sentences from " + sentenceFilename + ".");

		try (Stream<String> stream = Files.lines(Paths.get(sentenceFilename))) {
			stream.forEach(OpenNLPTagTest::process);
		}
		if (collectingUnknowns) {
			nfw.save();
		}
		double avgTagTime = (double) totalTagTime / (double) guesses;
		double avgSentenceLength = (double) loadedWords / (double) loadedSentences;
		log.info("Finished testing. Results:");
		log.debug("processed sentences:" + loadedSentences);
		log.debug("correct tags " + correct + "/" + guesses);
		log.debug("unknowns tags " + unknowns);
		log.debug("unknowns tags freqs:" + unknownTagFreqs);
		log.debug("loaded words " + loadedWords);
		log.debug("avg sentence tag time " + avgTagTime);
		log.debug("avg sentence length " + avgSentenceLength);
	}

	private static void process(String line) {
		TagSentence ref = new TagSentence();
		String[] split = line.split(" ");
		if (split.length < 2) {
			//this is likely an empty line..
			return;
		}
		loadedSentences++;

		if (loadedSentences % 50000 == 0) {
			log.debug("processed sentences:" + loadedSentences + " correct tags " + correct + "/" + guesses + ", loaded " + loadedWords + ", unknowns:" + unknowns);
			nfw.save();
		}

		List<String> words = new ArrayList<>();
		for (String s : split) {
			String[] wordAndTag = s.split("_");
			String word = wordAndTag[0];
			List<String> bases = vc.basesFor(word);
			if (bases.size() > 0) {
				//uses first base form to get reproducible results. assume first is always the same, as in sorted..
				word = bases.get(0);
			}
			String tag = wordAndTag[1];

			words.add(word);
			ref.add(new WordTag(word, tag));
			loadedWords++;
		}
		if (collectingUnknowns) {
			for (String word : words) {
				vc.basesFor(word);
			}
			return;
		}
		String[] array = new String[words.size()];
		array = words.toArray(array);
		long start = System.currentTimeMillis();
		String[] predicted = tagger.tag(array);
		long end = System.currentTimeMillis();
		long diff = end - start;
		totalTagTime += diff;
		int wordCount = words.size();
		for (int i = 0 ; i < wordCount ; i++) {
			String predictedTag = predicted[i];
			String referenceTag = ref.getWordTags().get(i).tag;
			if (predictedTag.equals(referenceTag)) {
				correct++;
			} else {
				if (predictedTag.equals("UNKNOWN")) {
					unknowns++;
					int freq = unknownTagFreqs.getOrDefault(referenceTag, 0);
					unknownTagFreqs.put(referenceTag, freq + 1);
				}
			}
			guesses++;
		}

	}
}

