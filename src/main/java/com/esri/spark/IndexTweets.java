package com.esri.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import twitter4j.auth.Authorization;
import twitter4j.auth.AuthorizationFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.esri.spark.pojo.Tweet;

public class IndexTweets {

    public static void main(String[] args) throws Exception {
        
        // Twitter4J
        // IMPORTANT: ajuster vos clés d'API dans twitter4J.properties
        Configuration twitterConf = ConfigurationContext.getInstance();
        Authorization twitterAuth = AuthorizationFactory.getInstance(twitterConf);
        SentimentAnalyzer.init();
        
        // Jackson
        ObjectMapper mapper = new ObjectMapper();

        // Language Detection
        DetectorFactory.loadProfile("src/main/resources/profiles");

        // Spark
        SparkConf sparkConf = new SparkConf()
                .setAppName("Tweets Android")
                .setMaster("local[2]")
                .set("spark.serializer", KryoSerializer.class.getName())
                .set("es.nodes", "localhost:9200");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(5000));
       
        String[] filters = { "#Android" };
        TwitterUtils.createStream(sc, twitterAuth, filters)
                .map(s -> new Tweet(s.getUser().getName(), s.getText(), s.getCreatedAt(), detectLanguage(s.getText()), findSentiment(s.getText())))
                .map(t -> mapper.writeValueAsString(t))
                .foreachRDD(tweets -> {                  
                    tweets.collect().stream().forEach(t -> System.out.println(t));
                    JavaEsSpark.saveJsonToEs(tweets, "spark/tweets-sentiments");
                    return null;
                });

        sc.start();
        sc.awaitTermination();
    }

    private static String detectLanguage(String text) throws Exception {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.detect();
    }
    
    private static int findSentiment(String text) throws Exception {
    	return SentimentAnalyzer.findSentiment(text);	
    }
}
