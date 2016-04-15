package com.bgu.dsp.worker.parser;
import java.util.*;

import java.util.List;
import java.util.Properties;

import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import com.bgu.dsp.common.protocol.managertoworker.TweetAnalyzer;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
public class NLPParser implements TweetAnalyzer{

static {
		init();
	}


	private static Set<String> ENTITY_TYPES;
	private static void init() {
		ENTITY_TYPES = new HashSet<>();
		ENTITY_TYPES.add("PERSON");
		ENTITY_TYPES.add("LOCATION");
		ENTITY_TYPES.add("ORGANIZATION");
	}

	public static void main(String[] args) {
		String tweet = "RT @AlexisMateo79: Tonight at Hamburger Marys Tampa http://t.co/5cj4eRB0RV";
		int sentiment = getSentiment(tweet);
		System.out.println("Sentiment is " + sentiment);

		System.out.println("Entities:\n" + getEntities(tweet));
	}

	public static int getSentiment(String tweet) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, parse, sentiment");
		StanfordCoreNLP  sentimentPipeline =  new StanfordCoreNLP(props);
		int mainSentiment = 0;
		if (tweet != null && tweet.length() > 0) {
			int longest = 0;
			Annotation annotation = sentimentPipeline.process(tweet);
			for (CoreMap sentence : annotation
					.get(CoreAnnotations.SentencesAnnotation.class)) {
				Tree tree = sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
				int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
				String partText = sentence.toString();
				if (partText.length() > longest) {
					mainSentiment = sentiment;
					longest = partText.length();
				}

			}
		}
		return mainSentiment;
	}



	public static Set<String> getEntities(String tweet){
		Set<String> res = new HashSet<>();
		Properties props = new Properties();
		props.put("annotators", "tokenize , ssplit, pos, lemma, ner");
		StanfordCoreNLP NERPipeline =  new StanfordCoreNLP(props);

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(tweet);

		// run all Annotators on this text
		NERPipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for(CoreMap sentence: sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the NER label of the token
				String ne = token.get(NamedEntityTagAnnotation.class);
				if (ENTITY_TYPES.contains(ne) ) {
					res.add(word + ":" + ne);
				}
			}
		}

		return res;

	}

	@Override
	public Tweet analyze(String tweet) {
		int sentiment = getSentiment(tweet);
		LinkedList<String> entities = new LinkedList<>(getEntities(tweet));
		return new Tweet(tweet,
				entities,
				sentiment);
	}
}
