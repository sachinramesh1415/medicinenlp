package com.mavenproject;


import com.mavenproject.domain.Output;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicineQueryNlp
{
    ArrayList<String> conceptList = new ArrayList<>();
    ArrayList<String> diseaseList = new ArrayList<>();
    ArrayList<String> medicationList = new ArrayList<>();
    ArrayList<String> symptomList = new ArrayList<>();
    ArrayList<String> causeList = new ArrayList<>();
    ArrayList<String> transmissionList = new ArrayList<>();
    ArrayList<String> typeList = new ArrayList<>();
    ArrayList<String> riskList = new ArrayList<>();
    public MedicineQueryNlp(){
        conceptList.add("medication");
        conceptList.add("disease");
        conceptList.add("cause");
        conceptList.add("symptom");
        conceptList.add("transmit");
        conceptList.add("risk");
        diseaseList.add("cancer");
        diseaseList.add("fever");
        diseaseList.add("dengue fever");
        diseaseList.add("chicken pox");
        medicationList.add("ibuprofen");
        medicationList.add("paracetamol");
        symptomList.add("cold");
        symptomList.add("headache");
        symptomList.add("pain");
        causeList.add("bacteria");
        causeList.add("virus");
        transmissionList.add("mosquito");
        transmissionList.add("airborne");
        typeList.add("infection");
        riskList.add("Exposure to milk");
        riskList.add("breast milk");
    }
    private String[] stopWords = new String[]{"what","and","like","taken","also","for","it", "with","who","which","required","used","do","is","?", "by","take","are", "give", "of", "in", "times","me","How","many", "the","if","a","has","getting"};
    private static Properties properties;
    private static String propertiesName = "tokenize, ssplit, pos, lemma";
    private static StanfordCoreNLP stanfordCoreNLP;
    static {
        properties = new Properties();
        properties.setProperty("annotators", propertiesName);
    }
    public static StanfordCoreNLP getPipeline() {
        if (stanfordCoreNLP == null) {
            stanfordCoreNLP = new StanfordCoreNLP(properties);
        }
        return stanfordCoreNLP;
    }
    public String getTrimmedQuery(String query) {
        String trimmedQuery = query.trim();
        trimmedQuery = trimmedQuery.replaceAll("\\s+", " ");
        trimmedQuery = trimmedQuery.replaceAll("\\t", " ");
        trimmedQuery = trimmedQuery.replaceAll("[?.]","");
        return trimmedQuery;
    }
    public ArrayList<String> getListWithoutStopWords(String query) {
        String trimmedQuery = getTrimmedQuery(query);
        String[] wordsSplitArray = trimmedQuery.split(" ");
        ArrayList<String> wordsSplitList = new ArrayList<String>();
        for (int i = 0; i < wordsSplitArray.length; i++) wordsSplitList.add(wordsSplitArray[i]);
        for (int i = 0; i < stopWords.length; i++) {
            for (int j = 0; j < wordsSplitList.size(); j++) {
                if (wordsSplitList.get(j).equalsIgnoreCase(stopWords[i].trim())) {
                    wordsSplitList.remove(wordsSplitList.get(j));
                }
            }
        }
        return wordsSplitList;
    }
    public List<String> getLemmatizedList(String query) {

        List<String> lemmatizedWordsList = new ArrayList<String>();
        ArrayList<String> listWithoutStopWords = getListWithoutStopWords(query);
        String stringWithoutStopWords = "";
        for (int i = 0; i < listWithoutStopWords.size(); i++) {
            stringWithoutStopWords = stringWithoutStopWords + listWithoutStopWords.get(i) + " ";
        }
        Annotation document = new Annotation(stringWithoutStopWords);
        StanfordCoreNLP stanfordCoreNLP = getPipeline();
        // run all Annotators on this text
        stanfordCoreNLP.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmatizedWordsList.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmatizedWordsList;
    }
    public Output RedisMatcher(String lemmatizedString)
    {
        Output output = new Output();
        output.setDomain("Medical");

        String symptoms="";
        for(int i=0; i < symptomList.size(); i++ ){
            Pattern pattern = Pattern.compile(symptomList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("symptom ", "");
                symptoms = symptoms + ", " + symptomList.get(i);
                lemmatizedString = lemmatizedString.replaceAll(symptomList.get(i),"");
            }
        }
        if(!(symptoms.equals(""))) output.setConstraint("symptom", symptoms.substring(2,symptoms.length()));

        int flag=0;
        String disease="";
        for(int i=0; i < diseaseList.size(); i++ ){
            Pattern pattern = Pattern.compile(diseaseList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                if(flag==0)
                {
                    lemmatizedString = lemmatizedString.replaceAll("disease ", "");
                    disease = diseaseList.get(i);
                    flag =1;
                }
                else {
                    if(disease.length()<diseaseList.get(i).length())
                    {
                        disease = diseaseList.get(i);
                    }
                }
            }
        }
        if(flag==1)
        {
            output.setConstraint("disease", disease);
        }

        for(int i=0; i < causeList.size(); i++ ){
            lemmatizedString = lemmatizedString.replaceAll("bacterium", "bacteria");
            Pattern pattern = Pattern.compile(causeList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("cause ", "");
                output.setConstraint("cause", causeList.get(i));
            }
        }

        for(int i=0; i<transmissionList.size();i++)
        {
            Pattern pattern = Pattern.compile(transmissionList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("transmit ", "");
                output.setConstraint("transmission", transmissionList.get(i));
            }
        }

        for(int i=0; i<typeList.size();i++)
        {
            Pattern pattern = Pattern.compile(typeList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("type ", "");
                output.setConstraint("type", typeList.get(i));
            }
        }

        String risks="";
        for(int i=0; i<riskList.size();i++)
        {
            Pattern pattern = Pattern.compile(riskList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("risk ", "");
                risks = risks + ", " + riskList.get(i);
                lemmatizedString = lemmatizedString.replaceAll(riskList.get(i),"");
            }
        }
        if(!(risks.equals(""))) output.setConstraint("risk factors", risks);

        String concepts="";
        for (int i=0; i<conceptList.size();i++)
        {
            Pattern pattern = Pattern.compile(conceptList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                concepts = concepts + ", " + conceptList.get(i);
            }
        }
        output.setQueryResult(concepts.substring(2,concepts.length()));

        return output;
    }
}
