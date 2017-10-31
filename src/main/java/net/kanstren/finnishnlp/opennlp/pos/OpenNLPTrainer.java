package net.kanstren.finnishnlp.opennlp.pos;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerFactory;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.postag.WordTagSampleStream;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Trains an OpenNLP POS tagger from the given input file and writes the POS model to the given output file.
 *
 * @author Teemu Kanstren
 */
public class OpenNLPTrainer {
	public static void main(String[] args) throws Exception {
		InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File(args[0]));
		ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, "UTF-8");
		ObjectStream<POSSample> sampleStream = new WordTagSampleStream(lineStream);

		POSModel model = POSTaggerME.train("fi", sampleStream, TrainingParameters.defaultParams(), new POSTaggerFactory());

		OutputStream modelOut = new BufferedOutputStream(new FileOutputStream(args[1]));
		model.serialize(modelOut);
	}
}
