package org.com.code.certificateProcessor.LangChain4j.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import org.com.code.certificateProcessor.LangChain4j.config.AgentConfig;
import org.com.code.certificateProcessor.LangChain4j.config.Model;
import org.com.code.certificateProcessor.LangChain4j.config.ModelConfig;
import org.com.code.certificateProcessor.LangChain4j.config.ModelConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

@Service
public class OCRService {

    private final ChatModel ocrModel;

    @Autowired
    public  OCRService(ModelConst modelConst) {
        this.ocrModel = Model.useQwen().withConfig(
                ModelConfig.builder()
                        .modelName(ModelConst.DashScopeModel.qwen3VLFlash)
                        .apiKey(modelConst.getDashScopeApiKey())
                        .responseFormat(ResponseFormat.JSON)
        ).build();
    }
    private static final String prompt = "请首先判断图片是否为一张奖状或证书。\n" +
            "\n" +
            "- **如果图片内容不是奖状或证书**（例如，是风景、动物、日常照片等）：\n" +
            "  请严格按照如下 JSON 格式输出：\n" +
            "    {\n" +
            "      \"ifCertification\": \"No\"\n" +
            "    }\n" +
            "\n" +
            "- **如果是奖状或证书**：\n" +
            "  请首先判断图片内容是否清晰到足以准确识别文字。\n" +
            "\n" +
            "  - **如果图片是奖状或证书，但内容严重模糊、失焦、或因光线/遮挡导致无法辨认**：\n" +
            "    请严格按照如下 JSON 格式输出，不要尝试提取任何内容：\n" +
            "    {\n" +
            "      \"ifCertification\": \"Yes\",\n" +
            "      \"isReadable\": \"No\",\n" +
            "    }\n" +
            "\n" +
            "  - **如果图片是清晰可读的奖状或证书**：\n" +
            "    请识别图片中的信息，并严格按照如下 JSON 格式输出。\n" +
            "    - **重要：** 如果图片中**没有**找到对应的字段（例如，通篇没有发现人名），则该字段的值**必须**为 `null`。**不要猜测或编造内容**。\n" +
        "    - 如果图片中的字段可能是 和人的姓名相关的，请将它们统一提取到 \"studentName\" 字段中。\n" +
                "    - 如果图片中的字段可能是 和奖状或者证书名称相关的，请将它们统一提取到 \"awardName\" 字段中。\n" +
                "    - 如果图片中的字段可能是 和奖状或者证书颁布时间相关的,请将它们统一提取到 \"awardDate\" 字段中。 \n" +
                "    - 日期必须被严格格式化为 YYYY-MM-DD 标准格式。\n" +
                "    - 如果日期缺少“日”（例如 \"2025年7月\"），则默认为该月的1号，输出 \"2025-07-01\"。\n" +
                "    - 如果日期缺少“月”和“日”（例如 \"2025年\"），则默认为该年的1月1日，输出 \"2025-01-01\"。\n" +
                "    - 如果图片中没有任何与日期相关的字段,则 \"awardDate\" 字段的值应为 `null`。\n" +
                "    - 格式示例（内容完整）：\n" +
                "      {\n" +
                "        \"studentName\": \"学生姓名\",\n" +
                "        \"awardName\": \"奖项名称\",\n" +
                "        \"awardDate\": \"2025-12-09\",\n" +
                "        \"ifCertification\": \"Yes\",\n" +
                "        \"isReadable\": \"Yes\"\n" +
                "      }\n" +
                "    - 格式示例（缺少奖项名称和日期）：\n" +
                "      {\n" +
                "        \"studentName\": \"张三\",\n" +
                "        \"awardName\": null,\n" +
                "        \"awardDate\": null,\n" +
                "        \"ifCertification\": \"Yes\",\n" +
                "        \"isReadable\": \"Yes\"\n" +
                "      }\n" +
                "    }";

    public String getOCRResult(String imageURL) throws NoApiKeyException, UploadFileException {
        // 构建基于 URL 的 Image 对象
        Image image = Image.builder()
                .url(imageURL)
                .build();

        Content prompt = TextContent.from(OCRService.prompt);
        Content imageContent = ImageContent.from(image, ImageContent.DetailLevel.MEDIUM);

        ChatMessage userMessage = UserMessage.builder().addContent(prompt).addContent(imageContent).build();
        return ocrModel.chat(userMessage).aiMessage().text();
    }
}