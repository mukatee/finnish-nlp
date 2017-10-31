package net.kanstren.finnishnlp.opennlp.pos;

/**
 * A word-tag pair.
 *
 * @author Teemu Kanstren
 */
public class WordTag {
  /** The word that is tagged. */
  public final String word;
  /** Associated tag for the word. So this is what POS the word has been tagged with. */
  public final String tag;

  public WordTag(String word, String tag) {
    this.word = word;
    this.tag = tag;
  }

  @Override
  public String toString() {
    return "WordTag{" +
        "word='" + word + '\'' +
        ", tag='" + tag + '\'' +
        '}';
  }
}
