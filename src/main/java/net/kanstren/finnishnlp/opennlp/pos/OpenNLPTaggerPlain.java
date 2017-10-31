package net.kanstren.finnishnlp.opennlp.pos;

import net.kanstren.finnishnlp.opennlp.voikko.NotFoundWords;
import net.kanstren.finnishnlp.opennlp.voikko.VoikkoConverter;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Tags given sentence using a pre-trained OpenNLP tagger.
 * Spits the sentence to tag to words using space character ' '.
 * This has issues, for example, to identify end punctuation.
 * Improve as needed.
 *
 * @author Teemu Kanstren
 */
public class OpenNLPTaggerPlain {
	private static final Logger log = LogManager.getLogger();

	public static void main(String[] args) throws Exception {
		log.info("Starting ONLP predictor");
		String modelFilename = args[0];
		log.info("Loading model '" + modelFilename + "'.");
		InputStream modelIn = new FileInputStream(modelFilename);
		POSModel model = new POSModel(modelIn);
		POSTaggerME tagger = new POSTaggerME(model);
		log.info("Model loaded.");

		String sentence = args[1];
		String[] words = sentence.split(" ");

		String[] predicted = tagger.tag(words);
		String output = "";
		for (int i = 0 ; i < words.length ; i++) {
			String tag = predicted[i];
			output += words[i]+"_"+tag+" ";
		}
		System.out.println(sentence+" -> "+output.trim());


	}
}

