package org.com.code.certificateProcessor.LangChain4j.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.com.code.certificateProcessor.LangChain4j.config.ModelConfig;

public interface ModelFactory {

    ChatModel createChatModel(ModelConfig config);

    StreamingChatModel createStreamingChatModel(ModelConfig config);

    EmbeddingModel createEmbeddingModel(ModelConfig config);
}
