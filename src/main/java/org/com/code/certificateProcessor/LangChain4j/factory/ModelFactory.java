package org.com.code.certificateProcessor.LangChain4j.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.com.code.certificateProcessor.LangChain4j.config.*;

public interface ModelFactory {

    ChatModel createChatModel(ChatModelConfig config);

    StreamingChatModel createStreamingChatModel(StreamingModelConfig config);

    EmbeddingModel createEmbeddingModel(EmbeddingModelConfig config);
    
    ScoringModel createScoringModel(ScoringModelConfig config);
}
