package net.kanstren.finnishnlp.word2vec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.util.Collection;

/**
 * Loads a Deeplearning4j word2vec model and prints 10 closest words to given word.
 *
 * Use as java net.kanstren.finnishnlp.word2vec modelname word
 *
 * @author Teemu Kanstren
 */
public class WikiWord2VecTester {
	private static final Logger log = LogManager.getLogger();

	public static void main(String[] args) throws Exception {
		String modelFilename = args[0];
		String word = args[1];
		System.out.println("trying to load model from file:"+modelFilename);
		//using "true" parameters value here as it seemed to at least successfully load the model, while the docs were completely broken..
		Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(modelFilename, true);
		Collection<String> nearest = word2Vec.wordsNearest(word, 10);
		for (String near : nearest) {
			System.out.println(word+" vs "+near+" = "+ word2Vec.similarity(word, near));
		}

	}
}
