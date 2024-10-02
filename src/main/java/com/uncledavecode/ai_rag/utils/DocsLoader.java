package com.uncledavecode.ai_rag.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocsLoader {

    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;

    @Value("classpath:docs/jls21.pdf")
    private Resource pdfResource;

    public DocsLoader(JdbcClient jdbcClient, VectorStore vectorStore) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadDocs(){
        var count = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class)
                .single();

        if(count == 0){
            log.info("Loading docs into vector store");
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopTextLinesToDelete(0)
                            .build()
                    )
                    .withPagesPerDocument(1)
                    .build();

            var pdfReader = new PagePdfDocumentReader(pdfResource, config);
            var result = pdfReader.get().stream()
                    .peek(doc -> log.info("Loading doc: {}", doc.getContent()))
                    .toList();

            vectorStore.accept(result);

            log.info("Loaded {} docs into vector store", result.size());
        }
    }
}
