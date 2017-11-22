package net.kanstren.finnishnlp.lda;

import net.kanstren.finnishnlp.opennlp.Config;
import net.kanstren.finnishnlp.opennlp.voikko.NotFoundWords;
import net.kanstren.finnishnlp.opennlp.voikko.VoikkoConverter;
import net.kanstren.finnishnlp.word2vec.WikiWord2VecPlain;
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
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads a Finnish wikipedia dump file, cleans it, tokenizes to sentences and words, runs word2vec on all the sentences.
 *
 * @author Teemu Kanstren
 */
public class WikiLDAPreProcess {
	private static final Logger log = LogManager.getLogger();
	/** Grabbing sentences from doc. */
	public static Voikko voikko;
	/** Tokenize words from sentences. */
	public static Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
	/** Keep track how many words in all docs/sentences. */
	private static int totalWords = 0;
	/** Keep track how many sentences in all docs. */
	private static int totalSentences = 0;
	private static NotFoundWords nfw = new NotFoundWords();
	/** Wrapper on Voikko to handle special cases, track words not found by Voikko, use custom spells, etc. */
	private static VoikkoConverter vc = new VoikkoConverter(nfw);

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
		int docs = 0;
		for (String article : dumper) {
			docs++;
			FileWriter fw = new FileWriter("wikidump/article"+docs+".txt");
			//first clean the wikipedia crufts
			String content = cleaner.clean(article);
			//remove all special chars left, except ones typically used to find sentence boundaries
			content = clean(content);
			List<Sentence> sentences1 = voikko.sentences(content);
			List<String> written = new ArrayList<>();
			for (Sentence sentence : sentences1) {
				String cleaned = clean2(sentence);
				String[] words = WikiWord2VecPlain.tokenizer.tokenize(cleaned);
				StringBuilder sb = new StringBuilder();
				for (int i = 0 ; i < words.length ; i++) {
					//ask Voikko for baseforms of each word in sentence, and replace the word with the baseform. this is our lemmatization here..
					//note that the first basesform might not be exactly right in every scenario but it serves the purpose here
					String base1= vc.basesFor2(words[i]).get(0);
					base1 = WikiWord2VecPlain.numbers(base1);
					sb.append(base1+" ");
				}
				written.add(sb.toString());
				fw.write(sb.toString()+"\n");
			}
			System.out.println("article "+docs+":\n"+written);
			fw.close();
		}
		System.out.println("docs:"+docs);
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
}
