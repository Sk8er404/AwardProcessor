package org.com.code.certificateProcessor.LangChain4j.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.com.code.certificateProcessor.LangChain4j.factory.ModelFactory;
import org.com.code.certificateProcessor.LangChain4j.factory.QwenFactory;

public class Model {
    private ModelFactory factory;
    private ModelConfig config;

    private Model(ModelFactory factory) {
        this.factory = factory;
    }

    public static Model useQwen() {
        return new Model(new QwenFactory());
    }

    public Model withConfig(ModelConfig config) {
        this.config = config;
        return this;
    }

    public ChatModel build() {
        return factory.createChatModel(config);
    }

    public StreamingChatModel buildStreaming() {
        return factory.createStreamingChatModel(config);
    }

    public EmbeddingModel buildEmbeddingModel() {
        return factory.createEmbeddingModel(config);
    }
}
