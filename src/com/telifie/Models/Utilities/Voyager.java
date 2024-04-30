package com.telifie.Models.Utilities;

import com.telifie.Models.Article;
import com.telifie.Models.Clients.Articles;
import org.bson.Document;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.ops.transforms.Transforms;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Voyager {

    private List<String> tokens = new ArrayList<>();
    private List<List<Integer>> tokenizedSequences = new ArrayList<>();
    private Vocabulary vocabulary = new Vocabulary();
    private int maxLength = 1000; // Set the maximum sequence length
    private static Word2Vec model;

    public Voyager(boolean load){
        if(load){
            try {
                new Modeling().loadModel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            begin();
        }
    }

    private void begin(){
        tokenize();
        buildVocabulary();
        performSequencePadding();
        Modeling word2VecTraining = new Modeling();
        try {
            word2VecTraining.loadModel();
//            word2VecTraining.trainWord2VecModel(tokenizedSequences);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs through all documents and tokenizes them.
     */
    private void tokenize(){
        TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        Articles client = new Articles(new Session("", "telifie")); //TODO modify query
        for (Article article : client.get(new Document("", ""))) {
            String content = article.getContent();
            Tokenizer tokenizer = tokenizerFactory.create(content);
            while (tokenizer.hasMoreTokens()) {
                tokens.addAll(tokenizer.getTokens());
            }
        }
        for (String token : tokens) {
            List<Integer> sequence = new ArrayList<>();
            sequence.add(vocabulary.getIndex(token));
            tokenizedSequences.add(sequence);
        }
    }

    private List<String> tokenize(String text){
        List<String> t2 = new ArrayList<>();
        TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        Tokenizer tokenizer = tokenizerFactory.create(text);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            t2.add(token);
            List<Integer> sequence = new ArrayList<>();
            sequence.add(vocabulary.getIndex(token));
            tokenizedSequences.add(sequence);
        }
        return t2;
    }

    private void buildVocabulary() {
        for (String token : tokens) {
            vocabulary.addWord(token);
        }
    }

    private void performSequencePadding() {
        INDArray sequenceArray = Nd4j.create(tokenizedSequences.size(), maxLength);
        for (int i = 0; i < tokenizedSequences.size(); i++) {
            List<Integer> sequence = tokenizedSequences.get(i);
            int sequenceLength = Math.min(sequence.size(), maxLength);
            for (int j = 0; j < sequenceLength; j++) {
                sequenceArray.putScalar(new int[]{i, j}, sequence.get(j));
            }
        }

        for (int i = 0; i < sequenceArray.size(0); i++) {
            int currentLength = (int) sequenceArray.size(1);
            if (currentLength < maxLength) {
                INDArray paddedSequence = padSequence(sequenceArray.getRow(i));
                sequenceArray.putRow(i, paddedSequence);
            } else if (currentLength > maxLength) {
                INDArray truncatedSequence = sequenceArray.getRow(i).get(NDArrayIndex.interval(0, maxLength));
                sequenceArray.putRow(i, truncatedSequence);
            }
        }
    }

    private INDArray padSequence(INDArray sequence) {
        int currentLength = (int) sequence.size(1);
        int numPadding = maxLength - currentLength;
        if (numPadding <= 0) {
            return sequence;
        }
        INDArray paddedSequence = Nd4j.zeros(DataType.INT, 1, maxLength);
        paddedSequence.get(NDArrayIndex.point(0), NDArrayIndex.interval(0, currentLength)).assign(sequence);
        return paddedSequence;
    }

    public class Vocabulary {
        private Map<String, Integer> wordToIndexMap = new HashMap<>();
        private Map<Integer, String> indexToWordMap = new HashMap<>();
        private int index = 0;

        public void addWord(String word) {
            if (!wordToIndexMap.containsKey(word)) {
                wordToIndexMap.put(word, index);
                indexToWordMap.put(index, word);
                index++;
            }
        }

        public int getIndex(String word) {
            return wordToIndexMap.getOrDefault(word, -1);
        }

        public String getWord(int index) {
            return indexToWordMap.getOrDefault(index, null);
        }

        public int size() {
            return wordToIndexMap.size();
        }
    }

    public class Modeling {

//        public void trainModel(List<List<Integer>> tokenizedSequences) throws IOException {
//            TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
//            model = new Word2Vec.Builder()
//                    .minWordFrequency(1)
//                    .iterations(5)
//                    .layerSize(100)
//                    .windowSize(5)
//                    .iterate(new Voyager.DocumentIterator(tokenizedSequences, tokenizerFactory))
//                    .build();
//            model.fit();
//            File modelFile = new File("path/to/word2vec_model.txt");
//            WordVectorSerializer.writeWord2VecModel(model, modelFile);
//        }

        public void loadModel() throws IOException {
            File modelFile = new File(Configuration.getModel());
            model = WordVectorSerializer.readWord2VecModel(modelFile);
        }
    }

    public class DocumentIterator implements Iterable<String> {
        private List<List<Integer>> tokenizedSequences;
        private TokenizerFactory tokenizerFactory;

        public DocumentIterator(List<List<Integer>> tokenizedSequences, TokenizerFactory tokenizerFactory) {
            this.tokenizedSequences = tokenizedSequences;
            this.tokenizerFactory = tokenizerFactory;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < tokenizedSequences.size();
                }

                @Override
                public String next() {
                    List<Integer> sequence = tokenizedSequences.get(index++);
                    return sequence.stream().map(Object::toString).collect(Collectors.joining(" "));
                }
            };
        }
    }

    public static class Unit {

        private String text;

        public Unit(String text){
            this.text = text;
        }

        public boolean isInterrogative(){
            String[] interrogativeWords = {"who", "what", "when", "where", "why", "how", "which", "whom", "whose", "whos"};
            for(String word : interrogativeWords){
                if(text.startsWith(word)){
                    return true;
                }
            }
            return false;
        }

        public String getSubject(){

            return "";
        }

        private double computeSimilarity(String word1, String word2) {
            INDArray wordVector1 = model.getWordVectorMatrix(word1);
            INDArray wordVector2 = model.getWordVectorMatrix(word2);
            return Transforms.cosineSim(wordVector1, wordVector2);
        }

        public String findPotentialSubject(List<String> tokens) {
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).matches("\\b(is|are|was|were|be|have|do)\\b")) {  // Simplified verb matching
                    if (i > 0) return tokens.get(i - 1);
                }
            }
            return "Subject not found";
        }

        public String findMainVerb(List<String> tokens) {
            for (String token : tokens) {
                if (token.matches("\\b(is|are|was|were|be|have|do)\\b")) {
                    return token;
                }
            }
            return "Verb not found";
        }
    }
}