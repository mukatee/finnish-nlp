package net.kanstren.finnishnlp.word2vec;

import net.kanstren.finnishnlp.opennlp.voikko.NotFoundWords;
import net.kanstren.finnishnlp.opennlp.voikko.VoikkoConverter;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.puimula.libvoikko.Sentence;
import org.wikiclean.WikiClean;
import org.wikiclean.WikipediaArticlesDump;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs word2vec on the Finnish wikipedia dump but uses Voikko to lemmatize all words and the POS tagger to tag them first.
 *
 * @author Teemu Kanstren
 */
public class WikiWord2VecVoikkoTagged {
	private static final Logger log = LogManager.getLogger();
	private static POSTaggerME tagger;
	private static NotFoundWords nfw = new NotFoundWords();
	/** Wrapper on Voikko to handle special cases, track words not found by Voikko, use custom spells, etc. */
	private static VoikkoConverter vc = new VoikkoConverter(nfw);

	public static void main(String[] args) throws Exception {
		String modelFilename = args[0];
		InputStream modelIn = new FileInputStream(modelFilename);
		POSModel model = new POSModel(modelIn);
		tagger = new POSTaggerME(model);

		WikipediaArticlesDump dumper = new WikipediaArticlesDump(new File("fiwiktionary-20171020-pages-meta-current.xml.bz2"));
		WikiClean cleaner = new WikiClean.Builder().build();
		List<String> tagSentences = new ArrayList<>();
		for (String article : dumper) {
			String content = cleaner.clean(article);
			content = WikiWord2VecPlain.clean(content);
			List<Sentence> sentences1 = WikiWord2VecPlain.voikko.sentences(content);
			for (Sentence sentence : sentences1) {
				String tagged = tagSentence(sentence);
				tagSentences.add(tagged);
				System.out.println(sentence.getText());
				System.out.println("->"+tagged);
			}

		}
		WikiWord2VecPlain.buildWord2Vec(tagSentences, "word2vec_voikko_pos.txt");
	}

	private static String tagSentence(Sentence sentence) {
		String sentenceText = sentence.getText();
		String str = WikiWord2VecPlain.numbers(sentenceText);
		String[] words = WikiWord2VecPlain.tokenizer.tokenize(str);
		String[] tags = tagger.tag(words);
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < words.length ; i++) {
			//build the word as in baseform_POSTAG. baseform is the lemma..of course it is, now drink your tea
			String base1= vc.basesFor2(words[i]).get(0);
			sb.append(base1+"_"+tags[i]+" ");
		}
		return sb.toString();
	}

}
