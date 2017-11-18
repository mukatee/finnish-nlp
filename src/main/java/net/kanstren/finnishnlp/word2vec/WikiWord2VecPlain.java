package net.kanstren.finnishnlp.word2vec;

import net.kanstren.finnishnlp.opennlp.Config;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.puimula.libvoikko.Sentence;
import org.puimula.libvoikko.Voikko;
import org.wikiclean.WikiClean;
import org.wikiclean.WikipediaArticlesDump;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads a Finnish wikipedia dump file, cleans it, tokenizes to sentences and words, runs word2vec on all the sentences.
 *
 * @author Teemu Kanstren
 */
public class WikiWord2VecPlain {
	private static final Logger log = LogManager.getLogger();
	/** Grabbing sentences from doc. */
	public static Voikko voikko;
	/** Tokenize words from sentences. */
	public static Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
	/** Keep track how many words in all docs/sentences. */
	private static int totalWords = 0;
	/** Keep track how many sentences in all docs. */
	private static int totalSentences = 0;

	static {
		//load voikko with the correct dictionary
		try {
			log.info("looking for dict in:"+ Config.VOIKKO_DICT_PATH);
			voikko = new Voikko("fi-x-morphoid", Config.VOIKKO_DICT_PATH);
		} catch (Exception e) {
			log.error("Failed to initialize Voikko", e);
			System.exit(1);
		}
	}

	public static void main(String[] args) throws Exception {
		//yes the filename is hardcoded, just change to args[0] or something..
		WikipediaArticlesDump dumper = new WikipediaArticlesDump(new File("fiwiktionary-20171020-pages-meta-current.xml.bz2"));
		//someone was nice enough to create a wikipedia clear to remove all the tags etc and just leave the text. thx
		WikiClean cleaner = new WikiClean.Builder().build();
		List<String> plainSentences = new ArrayList<>();
		for (String article : dumper) {
			//first clean the wikipedia crufts
			String content = cleaner.clean(article);
			//remove all special chars left, except ones typically used to find sentence boundaries
			content = clean(content);
			List<Sentence> sentences1 = voikko.sentences(content);
			for (Sentence sentence : sentences1) {
				String cleaned = clean2(sentence);
				plainSentences.add(cleaned);
				System.out.println(cleaned);
			}
		}
		buildWord2Vec(plainSentences, "word2vec_plain.txt");
		System.out.println("sentences:"+totalSentences);
		System.out.println("words:"+totalWords);
	}

	/**
	 * Remove remaining special chars and replace words with only numbrs as NUM
	 *
	 * @param sentence The process.
	 * @return Sentence after processing.
	 */
	private static String clean2(Sentence sentence) {
		totalSentences++;
		String sentenceText = sentence.getText().trim();
		//previously we would have left sentence end chars etc that voikko could use to find sentence boundaries
		//now we remove those as well
		sentenceText = clean3(sentenceText);
		String[] words = tokenizer.tokenize(sentenceText);
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < words.length ; i++) {
			String base1= words[i];
			//replace numbers with NUM to get general relations of other text to numbers
			base1 = numbers(base1);
			sb.append(base1+" ");
			totalWords++;
		}
		return sb.toString();
	}

	/** When cleaning a doc, we remove all these chars. */
	private static char[] badChars = new char[]
		{'$', '%', '#', '(', ')', '”', '”', ':', ';', '/', '\\', '"', '\'', '’', '…', '´', '´', '"', '*', '(', '\'', '•', '+', '\uDBC0', '\uDC78'};

	public static String clean(String doc) {
		char[] chars = doc.toCharArray();
		StringBuilder sb = new StringBuilder(doc.length());
		for (char c : chars) {
			//replace some forms of unicode dash with -
			if (c == 8212 || c == '–') {
				c = '-';
			} else {
				for (char bc : badChars) {
					if (bc == c) {
						c = ' ';
						break;
					}
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/** Second iteration of cleaning removes these chars. Left them after first to let Voikko split sentences better. */
	private static char[] badChars2 = new char[]
		{'.', ',', '!', '?', '\n', '\r'};

	public static String clean3(String text) {
		char[] chars = text.toCharArray();
		StringBuilder sb = new StringBuilder(text.length());
		for (char c : chars) {
			for (char bc : badChars2) {
				if (bc == c) {
					c = ' ';
					break;
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public static String numbers(String word) {
		//replace numbers with _NUM_
		try {
			Double.parseDouble(word);
			return "_NUM_";
		} catch (NumberFormatException e) {
		}
		return word;
	}

	public static void buildWord2Vec(List<String> sentences, String outputFilename) {
		SentenceIterator iter = new CollectionSentenceIterator(sentences);
		TokenizerFactory t = new DefaultTokenizerFactory();
		log.info("Building Word2Vec model....");
		Word2Vec vec = new Word2Vec.Builder()
			.minWordFrequency(5) //this was just a test run so you might want to play more with these params and others
			.iterations(1)
			.layerSize(100)
			.seed(42)
			.windowSize(5)
			.iterate(iter)
			.tokenizerFactory(t)
			.build();
		log.info("Fitting Word2Vec model....");
		vec.fit();
		log.info("Save vectors....");
		WordVectorSerializer.writeWord2VecModel(vec, outputFilename);
		log.info("Closest Words:");
		String word = "auto";
		Collection<String> nearest = vec.wordsNearest(word, 10);
		System.out.println("10 Words closest to "+word+": " + nearest);
		for (String near : nearest) {
			System.out.println(word+" vs "+near+" = "+ vec.similarity(word, near));
		}
	}
}
