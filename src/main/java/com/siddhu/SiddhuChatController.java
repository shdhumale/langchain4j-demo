package com.siddhu;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingConfiguration;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingStore;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor

public class SiddhuChatController {

    //private final ConversationalRetrievalChain conversationalRetrievalChain;
    private static Logger LOGGER = LoggerFactory.getLogger(SiddhuChatController.class);
    
 
    public EmbeddingModel embeddingModel() {
    	LOGGER.debug("Returning AllMiniLmL6V2EmbeddingModel Instance");
        return new AllMiniLmL6V2EmbeddingModel();
    }


    public AstraDbEmbeddingStore astraDbEmbeddingStore() {
        String astraToken = "<your-astradb-token>";
        String databaseId = "<your-database-id>";
        LOGGER.debug("Returning AstraDbEmbeddingStore");
        return new AstraDbEmbeddingStore(AstraDbEmbeddingConfiguration
                .builder()
                .token(astraToken)
                .databaseId(databaseId)
                .databaseRegion("us-east1")
                .keyspace("langchain4jsiddhu")
                .table("pdfchat")
                .dimension(384)
                .build());
    }

  
    public EmbeddingStoreIngestor embeddingStoreIngestor() {
    	LOGGER.debug("Returning EmbeddingStoreIngestor");
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel())
                .embeddingStore(astraDbEmbeddingStore())
                .build();
    }

  
    public ConversationalRetrievalChain conversationalRetrievalChain() {
        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(OpenAiChatModel.withApiKey("your-open-api-key"))
                .retriever(EmbeddingStoreRetriever.from(astraDbEmbeddingStore(), embeddingModel()))
                .build();
    }
 

    @PostMapping
    public String chatWithPdf(@RequestBody String text) {
        var answer = this.conversationalRetrievalChain().execute(text);
        LOGGER.debug("Answer provided by the Langchain4j using Astra as Vector DB is - {}", answer);
        return answer;
    }
}
