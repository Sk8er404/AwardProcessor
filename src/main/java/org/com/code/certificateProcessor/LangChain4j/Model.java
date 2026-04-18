package org.com.code.certificateProcessor.LangChain4j;

import org.com.code.certificateProcessor.LangChain4j.config.*;
import org.com.code.certificateProcessor.LangChain4j.factory.ModelFactory;
import org.com.code.certificateProcessor.LangChain4j.factory.OllamaFactory;
import org.com.code.certificateProcessor.LangChain4j.factory.QwenFactory;

public class Model<T> {
    private ModelFactory factory;
    private ModelConfig config;

    private Model(ModelFactory factory) {
        this.factory = factory;
    }


    public static <M> Model<M> useQwen() {
        return new Model<>(new QwenFactory());
    }

    public static <M> Model<M> useOllama() {
        return new Model<>(new OllamaFactory());
    }

    public Model<T> withConfig(ModelConfig config) {
        this.config = config;
        return this;
    }

    public T build() {
        if (config instanceof ChatModelConfig) {
            return (T)factory.createChatModel((ChatModelConfig) config);
        }
        else if(config instanceof StreamingModelConfig) {
            return (T)factory.createStreamingChatModel((StreamingModelConfig)config);
        }
        else if (config instanceof EmbeddingModelConfig) {
            return (T) factory.createEmbeddingModel((EmbeddingModelConfig) config);
        }
        else if (config instanceof ScoringModelConfig) {
            return  (T) factory.createScoringModel((ScoringModelConfig) config);
        }
        throw new IllegalArgumentException("Config 类型与工厂创建的模型类型不匹配");
    }
}
