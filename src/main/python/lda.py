import logging
from stop_words import get_stop_words
from gensim import corpora,models
import gensim
import os, operator
from collections import defaultdict

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

def topics_prct(lda, topic_count, word_count, corpus):
    log = logging.getLogger("bob") #the logger seems to require some id so i just call it "bob"
    topic_words = {}
    for t in range(topic_count):
        # top is now list of tuples (word, probability). topn=number of words to take
        top = lda.show_topic(t, topn=word_count)
        topic_words[t] = top

    topic_sizes = defaultdict(int)

    for doc in corpus:
        #doc_box is the document represented as a bag of words
        doc_bow = doc
        dist = lda[doc_bow]
        for topic_word in dist:
            # count topic sizes by summing the probabilities of all words in all docs to be in that topic
            # so the topic_word is in topic_id and it makes up "percent" of words in the document (instances of one word can be in different topics across the doc)
            topic_id = topic_word[0]
            percent = topic_word[1]
            topic_sizes[topic_id] += percent
    print("sized topics")

    topic_words_weighted = {}
    for t in range(topic_count):
        t_words = topic_words[t] #the top words in topic "t"
        topic_size = topic_sizes[t] #the total size of the topic "t", sum of its word probabilities
        tw_words = []
        topic_words_weighted[t] = tw_words
        for word, percent in t_words:
            my_tuple = (word, percent * topic_size) #build a comparable list of words/topic to represent their sizes. not exactly the number of appearances since I could not figure that for gensim, but serves the purpose
            tw_words.append(my_tuple)

    log.info("sized words")

    #sort the topics by their ID values, so if we print the list it will be in numerical order. could also sort by the topic size (item 1 I guess) to get biggest topics first
    sorted_topics = sorted(topic_sizes.items(), key=operator.itemgetter(0))

    file_data = ""

    for topic in sorted_topics:
        topic_id = topic[0]
        file_data += "topic"+str(topic_id)+"="
        tww = topic_words_weighted[topic_id]
        for tw in tww:
            word_size = int(tw[1]*100)
            file_data += tw[0]+"["+str(word_size)+"] "
        file_data += "\n"

    log.info("built file data")
    print(file_data)
    return file_data

def create_model(topic_count, doc_limit, iterations):
    stoplist = get_stop_words("fi")

    documents = []

    print("starting to read docs")
    count = 0
    #i put all the preprocessed (lemmatized, known cruft and stopwords removed) into "wikidump" dir, this would differ for different datasets, or you might just directly read from some source and preprocess here
    for filename in os.listdir("wikidump"):
        with open("wikidump/"+filename, "r") as file:
            data = file.read()
            #drop single letter words and words in the list of stop words, also lowercase all
            #i guess this might have an issue of removing stopwords in different upper/lowercase combos but but..
			#also, i did preprocess the docs before but good opportunity to fix here what was missed in preprocessing..
            doc = [word for word in data.lower().split() if (word not in stoplist and len(word) > 2)]
            documents.append(doc)
            count += 1
            if count > doc_limit:
                break

    print("finished reading docs")
    print("got "+str(count)+" docs. starting model build.")

    #this is just basic application of gensim according to gensim docs/tutorials
    dictionary = corpora.Dictionary(documents)
    print("dictionary built")
    corpus = [dictionary.doc2bow(text) for text in documents]
    print("corpus built")
    ldamodel = gensim.models.ldamodel.LdaModel(corpus, num_topics=topic_count, id2word = dictionary, passes=iterations)
    print("lda built")

    print(ldamodel.print_topics(num_topics=topic_count, num_words=10))
    return ldamodel, corpus

def write_file(txt):
    topics_file = open("topics.txt", "w")
    topics_file.write(txt)
    topics_file.close()

topic_count = 50
word_count = 10
doc_limit = 50000000 #you can use the doc_limit here to cut the used dataset to use only first N documents. useful to quickly test stuff
iterations = 2
lda_model, corpus = create_model(topic_count, doc_limit, iterations)
topics_txt = topics_prct(lda_model, topic_count, word_count, corpus)
write_file(topics_txt)




