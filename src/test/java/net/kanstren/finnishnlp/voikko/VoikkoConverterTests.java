package net.kanstren.finnishnlp.voikko;


import net.kanstren.finnishnlp.opennlp.voikko.NotFoundWords;
import net.kanstren.finnishnlp.opennlp.voikko.VoikkoConverter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren
 */
public class VoikkoConverterTests {
	private NotFoundWords nfw;
	private VoikkoConverter vc;

	@BeforeMethod
	public void setupTest() {
		nfw = new NotFoundWords();
		vc = new VoikkoConverter(nfw);
	}

	@Test
	public void notFound() {
		String word = "diibabaaa";
		List<String> bases = vc.basesFor(word);
		assertEquals(bases.size(), 0, "Voikko should not recognize '"+word+"'");
	}

	@Test
	public void notFound2() {
		String word = "diibabaaa";
		List<String> bases = vc.basesFor2(word);
		assertEquals(bases.size(), 1, "bases2() should return word itself when Voikko does not recognize it and no custom spell exists.");
		assertEquals(word, bases.get(0));
	}

	@Test
	public void customWordIsItself() {
		//expect the custom list to exist and contain 'serviisi=serviisi'
		String word = "serviisi";
		List<String> bases = vc.basesFor(word);
		assertEquals(bases.size(), 1, "Custom word should map to itself it so defined in custom spell list.");
		assertEquals(word, bases.get(0));
	}

	@Test
	public void bananaIsBanaani() {
		//expect the custom list to exist and contain 'banana=banaani'
		String word = "banana";
		List<String> bases = vc.basesFor(word);
		assertEquals(bases.size(), 1, "Custom word should map expected to contain '"+word+"'");
		assertEquals("banaani", bases.get(0));
	}

	@Test
	public void recursiveCustomWords() {
		//expect the custom list to exist and contain 'dingg=dongg' and 'dongg=dingg'
		String word = "dingg";
		List<String> bases = vc.basesFor(word);
		assertEquals(bases.size(), 0, "Recursive custom definition should give empty list.");
	}
}
