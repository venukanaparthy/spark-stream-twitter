package com.esri.spark;

import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class SentimentAnalyzer {
    static StanfordCoreNLP pipeline;

    public static void init() {
    	Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,parse,sentiment");
		props.setProperty("parse.model","edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        pipeline = new StanfordCoreNLP(props);
    }

    public static int findSentiment(String tweet) {

        int mainSentiment = 0;
		try{
        if (tweet != null && tweet.length() > 0) {
            int longest = 0;
            Annotation annotation = pipeline.process(tweet);
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
		}catch(Exception ex){
			System.err.println(ex.toString());
		}
        return mainSentiment;
    }
}
