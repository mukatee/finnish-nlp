package net.kanstren.finnishnlp.word2vec;

import net.kanstren.finnishnlp.opennlp.Config;
import net.kanstren.finnishnlp.opennlp.voikko.NotFoundWords;
import net.kanstren.finnishnlp.opennlp.voikko.VoikkoConverter;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.puimula.libvoikko.Sentence;
import org.puimula.libvoikko.Voikko;
import org.wikiclean.WikiClean;
import org.wikiclean.WikipediaArticlesDump;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs word2vec on the Finnish wikipedia dump but uses Voikko to lemmatize all words first.
 *
 * @author Teemu Kanstren
 */
public class WikiWord2VecVoikko {
	private static final Logger log = LogManager.getLogger();
	private static NotFoundWords nfw = new NotFoundWords();
	/** Wrapper on Voikko to handle special cases, track words not found by Voikko, use custom spells, etc. */
	private static VoikkoConverter vc = new VoikkoConverter(nfw);

	public static void main(String[] args) throws Exception {
		WikipediaArticlesDump dumper = new WikipediaArticlesDump(new File("fiwiktionary-20171020-pages-meta-current.xml.bz2"));
		WikiClean cleaner = new WikiClean.Builder().build();
		List<String> plainSentences = new ArrayList<>();
		for (String article : dumper) {
			String content = cleaner.clean(article);
			content = WikiWord2VecPlain.clean(content);
			List<Sentence> sentences1 = WikiWord2VecPlain.voikko.sentences(content);
			for (Sentence sentence : sentences1) {
				String cleaned = clean2(sentence);
				plainSentences.add(cleaned);
				System.out.println(cleaned);
			}
		}
		WikiWord2VecPlain.buildWord2Vec(plainSentences, "word2vec_voikko.txt");
	}

	private static String clean2(Sentence sentence) {
		String sentenceText = sentence.getText().trim();
		sentenceText = WikiWord2VecPlain.clean3(sentenceText);
		String[] words = WikiWord2VecPlain.tokenizer.tokenize(sentenceText);
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < words.length ; i++) {
			//ask Voikko for baseforms of each word in sentence, and replace the word with the baseform. this is our lemmatization here..
			//note that the first basesform might not be exactly right in every scenario but it serves the purpose here
			String base1= vc.basesFor2(words[i]).get(0);
			base1 = WikiWord2VecPlain.numbers(base1);
			sb.append(base1+" ");
		}
		return sb.toString();
	}

}
