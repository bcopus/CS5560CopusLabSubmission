package com.bjc;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.System.out;

public class Main {
  
  class Token {
    String text;
    String lemma;
    String pos;
    String ne;
  
    boolean lemmaEquivalent(Token t) {
      return this.lemma.equals(t.lemma);
    }
    
    @Override
    public String toString() {
      return text + " " + pos + " " + ne;
    }
  }//class Token
  
  class Sentence {
    ArrayList<Token> tokens;
    Sentence() {
      tokens = new ArrayList<>();
    }
    
    int similarity(Sentence s) {
      int similarity = 0;
      for(Token t : this.tokens)
        for(Token c : s.tokens)
          if(t.lemmaEquivalent(c))
            similarity++;
      return similarity;
    }//similarity()
    
    public String text() {
      StringBuffer sb = new StringBuffer();
      for(Token t : tokens)
        sb.append(t.text + " ");
      return sb.toString();
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for(Token t : tokens)
        sb.append("(" + t + ")");
      return sb.toString();
    }
  }//class Sentence
  
  class Document {
    ArrayList<Sentence> sentences;
    Document() {
      sentences = new ArrayList<>();
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for(Sentence s : sentences)
        sb.append(s + "\n");
      return sb.toString();
    }
  }//class Sentence
  
  private Document annotate(StanfordCoreNLP pipeline, String input) {
    Annotation document = new Annotation(input);
    pipeline.annotate(document);

    Document doc = new Document();
    StringBuffer sb = new StringBuffer();
    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    for (CoreMap sentence : sentences) {
      Sentence s = new Sentence();
      doc.sentences.add(s);
      for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        Token tok = new Token();
        s.tokens.add(tok);
        tok.text = token.get(CoreAnnotations.TextAnnotation.class);
        tok.lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
        tok.pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        tok.ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
      }
      sb.append("\n");
    }
    return doc;
  }//annotate()
  
  private Document annotateFile(StanfordCoreNLP pipeline, Path inFilePath) {
    StringBuffer sb = new StringBuffer();
    Object[] contents;
    try (BufferedReader reader = Files.newBufferedReader(inFilePath)) {
      if(reader.ready()) {
        contents = reader.lines().toArray();
        for(Object line : contents)
          sb.append((String)(line) + " ");
      }
    } catch (IOException e){
      out.println(e.getMessage());
    }
    return annotate(pipeline, sb.toString());
  }//annotateFile()
  
  String extractAnswer(Sentence question, Document answerBase) {
    //find the sentence in answerbase most similar to question;
    //extract and return a named entity from the sentence as follows:
    //  if the question WP = "who", return a person;
    //  if the question WP = "where", return a location;
    //  if the question WP = "when", return a time;
    //  if the question WP = "what", return anything that follows a be-verb.
    
    Sentence bestMatch = null;
    for(Sentence s : answerBase.sentences) {
      if(bestMatch == null || (s.similarity(question) > bestMatch.similarity(question)))
        bestMatch = s;
    }
    
    String wp = extractWh(question);
    String ans = null;
    if(wp == null) {
      //malformed question, probably the best we can do is return the closest sentential match
      return bestMatch.toString();
    } else {
      switch (wp) {
        case "who" :
          ans = extractThing(bestMatch, "PERSON");
          break;
        case "where" :
          ans = extractThing(bestMatch, "LOCATION");
          break;
        case "when" :
          ans = extractThing(bestMatch, "DATE");
          if(ans.equals(""))
            ans = extractThing(bestMatch, "TIME");
          break;
        default :
      }//switch
      
      if (ans == null || ans.equals(""))
        return bestMatch.text();
      else
        return ans;
    }//else
    
  }//extractAnswer()
  
  String extractThing(Sentence s, String type) {
    StringBuilder sb = new StringBuilder();
    for(Token t : s.tokens) {
      if (t.ne.equals(type)) {
        sb.append(t.lemma + " ");
      }
    }
    return sb.toString();
  }//extractThing()
  
  String extractWh(Sentence s) {
    for (Token t : s.tokens)
      if(t.pos.equals("WP") || t.pos.equals("WRB"))
        return t.lemma;
    return null;
  }//extractWP()
  
  public void run() {
    // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
  
    Path inFilePath = Paths.get("D:\\_kdm\\lab1.1\\in.txt");
    Document aBase=annotateFile(pipeline, inFilePath);
    out.println("Answer base:\n" + aBase);
    
    String[] questions = {
            "who is the president of the United States of America?",
            "what do you like to do in your garage?",
            "when do cows eat grass?",
            "where is trump the president?",
            "who was chased by paparazzi?"
    };
  
    for(String question : questions) {
      out.print("\nQuestion: " + question);
      Document qBase = annotate(pipeline, question);
      out.println(" [" + qBase.sentences.get(0) + "]");
      String answer = extractAnswer(qBase.sentences.get(0), aBase);
      out.println("Best Answer: " + answer);
    }
  }//run()
  
  public static void main(String args[]) {
    new Main().run();
  }//main()
  
}//class Main
