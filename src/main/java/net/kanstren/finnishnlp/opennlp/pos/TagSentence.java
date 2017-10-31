package net.kanstren.finnishnlp.opennlp.pos;

import java.util.ArrayList;
import java.util.List;

/**
 * A sentence with word-tag pairs showing which POS tag each word in the sentence has been given.
 *
 * @author Teemu Kanstren.
 */
public class TagSentence {
  private final List<WordTag> words = new ArrayList<>();

  public void add(WordTag wordTag) {
    words.add(wordTag);
  }

  public List<WordTag> getWordTags() {
    return words;
  }

  public String[] wordArray() {
    String[] array = new String[words.size()];
    int i = 0;
    for (WordTag word : words) {
      array[i++] = word.word;
    }
    return array;
  }

  @Override
  public String toString() {
    return "TagSentence{" +
        "words=" + words +
        '}';
  }
}
