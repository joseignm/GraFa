package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

abstract class Indexer {

    static IndexWriter makeWriter(String path, Analyzer analyzer) throws IOException {
        Path fDir = Paths.get(path);
        File file = fDir.toFile();

        if(file.exists()){
            if(file.isFile()){
                throw new IOException("Cannot open directory at "+path+" since its already a file.");
            }
        } else{
            if(!file.mkdirs()){
                throw new IOException("Cannot open directory at "+path+". Try create the directory manually.");
            }
        }

        Directory dir = FSDirectory.open(fDir);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(dir, iwc);
    }

    static String getField(IndexSearcher searcher, String subject, String field) throws IOException {
        Term term = new Term(DataFields.SUBJECT.name(), subject);
        Query query = new TermQuery(term);
        TopDocs results = searcher.search(query, 1);
        if(results.totalHits < 1) {
            // System.err.println("WARN: Subject "+subject+" does not exist or was not indexed.");
            return null;
        }
        ScoreDoc[] hits = results.scoreDocs;
        Document doc = searcher.doc(hits[0].doc);
        return doc.get(field);
    }

    static String[] getFieldAll(IndexSearcher searcher, String subject, String field) throws IOException {
        Term term = new Term(DataFields.SUBJECT.name(), subject);
        Query query = new TermQuery(term);
        TopDocs results = searcher.search(query, 1);
        if(results.totalHits < 1) {
            // System.err.println("WARN: Subject "+subject+" does not exist or was not indexed.");
            return null;
        }
        ScoreDoc[] hits = results.scoreDocs;
        Document doc = searcher.doc(hits[0].doc);
        return doc.getValues(field);
    }
}
