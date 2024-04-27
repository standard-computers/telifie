package com.telifie.Models.Utilities;

import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
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
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Voyager {

    private List<String> tokens = new ArrayList<>();
    private List<List<Integer>> tokenizedSequences = new ArrayList<>();
    private Vocabulary vocabulary = new Vocabulary();
    private int maxLength = 1000; // Set the maximum sequence length

    public Voyager(){
        begin();
    }

    private void begin(){
        tokenize();
        buildVocabulary();
        performSequencePadding();
        Word2VecTraining word2VecTraining = new Word2VecTraining();
        try {
            word2VecTraining.trainWord2VecModel(tokenizedSequences);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs through all documents and tokenizes them.
     */
    private void tokenize(){
        TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        ArticlesClient client = new ArticlesClient(new Session("", "telifie")); //TODO modify query
        for (Article article : client.get(new Document("", ""))) {
            String content = article.getContent();
            Tokenizer tokenizer = tokenizerFactory.create(content);
            while (tokenizer.hasMoreTokens()) {
                tokens.addAll(tokenizer.getTokens());
            }
        }
        for (String token : tokens) {
            List<Integer> sequence = new ArrayList<>();
            // Convert token to integer index using vocabulary
            sequence.add(vocabulary.getIndex(token));
            tokenizedSequences.add(sequence);
        }
    }

    private void buildVocabulary() {
        for (String token : tokens) {
            vocabulary.addWord(token);
        }
    }

    private void performSequencePadding() {
        // Convert tokenized sequences to INDArray
        INDArray sequenceArray = Nd4j.create(tokenizedSequences.size(), maxLength);
        for (int i = 0; i < tokenizedSequences.size(); i++) {
            List<Integer> sequence = tokenizedSequences.get(i);
            int sequenceLength = Math.min(sequence.size(), maxLength);
            for (int j = 0; j < sequenceLength; j++) {
                sequenceArray.putScalar(new int[]{i, j}, sequence.get(j));
            }
        }

        // Perform sequence padding directly on the INDArray
        for (int i = 0; i < sequenceArray.size(0); i++) {
            int currentLength = (int) sequenceArray.size(1);
            if (currentLength < maxLength) {
                // Pad the sequence with zeros to the maximum length
                INDArray paddedSequence = padSequence(sequenceArray.getRow(i));
                sequenceArray.putRow(i, paddedSequence);
            } else if (currentLength > maxLength) {
                // Truncate the sequence to the maximum length
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

    public class Word2VecTraining {

        public void trainWord2VecModel(List<List<Integer>> tokenizedSequences) throws IOException {
            TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
            Word2Vec vec = new Word2Vec.Builder()
                    .minWordFrequency(1)
                    .iterations(5)
                    .layerSize(100)
                    .windowSize(5)
                    .iterate((org.deeplearning4j.text.documentiterator.DocumentIterator) new DocumentIterator(tokenizedSequences, tokenizerFactory))
                    .build();
            vec.fit();
            File modelFile = new File("path/to/word2vec_model.txt");
            WordVectorSerializer.writeWord2VecModel(vec, modelFile);
        }
    }

    // DocumentIterator implementation to iterate over tokenized sequences
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
                    // Convert the list of integer indices to a space-separated string
                    return sequence.stream().map(Object::toString).collect(Collectors.joining(" "));
                }
            };
        }
    }
}