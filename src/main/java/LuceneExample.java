import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LuceneExample {
    private static final String INDEX_DIR = "/home/pirooz/myFiles/A_Programming/my-projects/luceneSearchEngine/src/index";
    private static final List<String> STOP_WORDS = List.of("a", "an", "the", "and", "or", "but");
    private static final CharArraySet STOP_SET = new CharArraySet(STOP_WORDS, true);

    public static void main(String[] args) throws IOException {
        // Generate the inverted index
        generateIndex();

        // Read the queries from the user
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a search query (use OR, AND, NOT operators and wildcard '*'): ");
        String queryString = scanner.nextLine();

        List<String> results = searchIndex(queryString);
        assert results != null;
        printResult(results);
    }

    private static void generateIndex() throws IOException {
        // Initialize index writer
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(FSDirectory.open(new File(INDEX_DIR).toPath()), config);

        // Index each document in the directory
        File[] files = new File(INDEX_DIR).listFiles();
        for (File file : files) {
            Document document = new Document();
            String text = new String(Files.readAllBytes(file.toPath()));
            document.add(new TextField("contents", text, Field.Store.YES));
            writer.addDocument(document);
        }

        // Commit and close the index writer
        writer.commit();
        writer.close();
    }



    private static List<String> searchIndex(String queryStr) throws IOException {
        // Tokenize the query string and remove stop words
        Analyzer analyzer = new StandardAnalyzer(STOP_SET);
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(queryStr));
        stream.reset();
        List<String> tokens = new ArrayList<>();
        while (stream.incrementToken()) {
            tokens.add(stream.getAttribute(CharTermAttribute.class).toString());
        }
        stream.end();
        stream.close();
        analyzer.close();

        // Create a boolean query with a should clause for each token
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        for (String token : tokens) {
            Term term = new Term("contents", token);
            Query termQuery = new TermQuery(term);
            queryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
        }
        Query query = queryBuilder.build();

        // Search the index and return the top hits
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(INDEX_DIR).toPath()));
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(query, 10);

            List<String> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                results.add(doc.get("contents"));
            }
            return results;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }




    private static void printResult(List<String> results) {
        if (results.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (int i = 0; i < results.size(); i++) {
                System.out.println("Result " + (i+1) + ":");
                System.out.println(results.get(i));
                System.out.println();
            }
        }
    }

}