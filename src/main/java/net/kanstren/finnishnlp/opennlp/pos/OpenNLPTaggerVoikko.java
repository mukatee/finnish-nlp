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
 * The same as {@link OpenNLPTaggerPlain} but uses Voikko to baseform every word of the sentence to tag before tagging.
 *
 * @author Teemu Kanstren
 */
public class OpenNLPTaggerVoikko {
	private static final Logger log = LogManager.getLogger();
	private static NotFoundWords nfw = new NotFoundWords();
	private static VoikkoConverter vc = new VoikkoConverter(nfw);

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
		for (int i = 0 ; i < words.length ; i++) {
			words[i] = vc.basesFor2(words[i]).get(0);
		}
		String[] predicted = tagger.tag(words);
		String output = "";
		for (int i = 0 ; i < words.length ; i++) {
			String tag = predicted[i];
			output += words[i]+"_"+tag+" ";
		}
		System.out.println(sentence+" -> "+output.trim());


	}
}

