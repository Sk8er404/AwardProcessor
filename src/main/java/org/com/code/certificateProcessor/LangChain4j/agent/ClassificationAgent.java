package org.com.code.certificateProcessor.LangChain4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.com.code.certificateProcessor.pojo.modelInfo.AwardClassification;
import org.com.code.certificateProcessor.pojo.modelInfo.AwardInfo;
import org.com.code.certificateProcessor.pojo.modelInfo.DeduplicationResult;
import org.com.code.certificateProcessor.pojo.entity.AwardSubmission;
import org.com.code.certificateProcessor.pojo.entity.StandardAward;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ClassificationAgent {

    /**
     * classifyAward 的任务是“归类”，而不是“查重”，因此日期字段在此处不参与判断。
     */
    @SystemMessage(
            "你是一个严谨且格式严格的教务系统审核助手。" +
                    "你的任务是：根据学生上传奖状的 OCR 识别信息，从一个“标准奖项候选列表”中匹配出最准确的标准奖项。" +
                    "无论是否找到匹配项，你都必须严格输出一个完整的 JSON 对象，其结构必须符合 AwardClassification 类的定义。" +
                    "如果 AwardClassification 的某些字段没有对应值，请将它们的值设为 null，而不是省略。"
    )
    @UserMessage(
            "请帮我分析以下数据：\n" +
                    "1. **学生奖状识别信息 (来自 OCR)**:\n" +
                    "```json\n{{ocrInfo}}\n```\n" +
                    "2. **标准奖项候选列表 (来自数据库搜索)**:\n" +
                    "```json\n{{candidates}}\n```\n" +
                    "**任务要求**:\n" +
                    "1. 对比学生奖状的 `awardName`（来自 OCR 信息）与候选列表中每个奖项的 `officialName`。\n" +
                    "2. 你需要输出一个完整的 `AwardClassification` JSON 对象，结构如下：\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"matchFound\": true 或 false,\n" +
                    "  \"matchedAwardId\": \"string 或 null\",\n" +
                    "  \"matchedAwardName\": \"string 或 null\",\n" +
                    "  \"reasoning\": \"string (说明匹配理由或无法匹配的原因)\"\n" +
                    "}\n" +
                    "```\n" +
                    "3. 输出时只返回这个 JSON 对象，**不要添加任何额外文本、解释或注释**。\n" +
                    "4. 若无法判断或没有合适匹配，请设置：\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"matchFound\": false,\n" +
                    "  \"matchedAwardId\": null,\n" +
                    "  \"matchedAwardName\": null,\n" +
                    "  \"reasoning\": \"无法根据 OCR 信息与候选列表确定匹配。\"\n" +
                    "}\n" +
                    "```"
    )
    AwardClassification classifyAward(
            @V("ocrInfo") AwardInfo ocrInfo,
            @V("candidates") List<StandardAward> candidates
    );


    /**
     * 奖项查重 Agent
     */
    @SystemMessage({
            "你是一个严格的教务系统查重助手。",
            "你的任务是对比一个“新提交的奖项”和一组“数据库中的旧奖项记录”，判断新奖项是否为其中某一个旧奖项的重复提交。",
            "你的输出必须是严格的 JSON 格式，结构必须符合 DeduplicationResult 类的定义。",
            "如果 DeduplicationResult 的某些字段没有对应值，请将其设置为 null，而不是省略。",
            "---",
            "**核心规则**:",
            "1. **日期不为空（完美匹配）:** 如果新奖项 `awardDate` **不为 null**：",
            "   - 只有当 `awardName` 和 `awardDate` (或日期在同一年度/月份) 都高度一致时，才认为是重复提交。",
            "   - 如果 `awardName` 相似，但 `awardDate` 明显不同（例如 '2023-01-01' vs '2024-01-01'），则**不是重复**。",
            "2. **日期为空（核心匹配）:** 如果新奖项 `awardDate` **为 null**：",
            "   a. 你必须在旧奖项列表中查找所有 `awardName` 相似的记录。",
            "   b. 如果在旧记录中只找到 **一条** `awardName` 相似的记录，则判定为 **重复** (高置信度匹配)。",
            "   c. 如果在旧记录中找到 **多条** `awardName` 相似的记录 (例如：同一个奖项在2023年和2024年的记录)，这属于“歧义”，此时**不能**判定为重复。",
            "   d. 如果在旧记录中找到 0 条 `awardName` 相似的记录，则**不是重复**。"
    })
    @UserMessage(
            "请分析以下数据：\n" +
                    "\n" +
                    "1. **新提交的奖项信息 (来自 OCR 提取)**:\n" +
                    "```json\n{{newAward}}\n```\n" +
                    "\n" +
                    "2. **数据库中的旧奖项记录列表**:\n" +
                    "```json\n{{oldAward}}\n```\n" +
                    "\n" +
                    "**任务要求**:\n" +
                    "1. 严格遵循系统消息中的“核心规则”进行判断。\n" +
                    "2. **如果 `newAward.awardDate` 为 null**：请切换到“核心匹配”策略。遍历 `oldAward` 列表，统计 `awardName` 相似的记录数量："+
            "   - 如果数量为 1，请设置 `duplicated: true`。"+
            "   - 如果数量为 0 或 大于 1 (存在歧义)，请设置 `duplicated: false`，并在 `reasoning` 中说明（例如：“新奖项缺少日期，且在旧记录中找到多条同名奖项，无法确认重复。”）。\n" +
                    "3. **如果 `newAward.awardDate` 不为 null**：请执行“完美匹配”策略，同时对比 `awardName` 和 `awardDate`。\n" +
                    "4. **如果判定为重复**，请返回：\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"duplicated\": true,\n" +
                    "  \"matchedAwardId\": \"匹配到的旧奖项ID\",\n" +
                    "  \"matchedAwardName\": \"匹配到的旧奖项名称\",\n" +
                    "  \"reasoning\": \"说明为什么认为两者是同一个奖项（例如：名称和日期都匹配，或名称匹配且日期为空但仅有唯一匹配项）\"\n" +
                    "}\n" +
                    "```\n" +
                    "5. **如果判定为不重复**，请返回：\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"duplicated\": false,\n" +
                    "  \"matchedAwardId\": null,\n" +
                    "  \"matchedAwardName\": null,\n" +
                    "  \"reasoning\": \"说明为什么判定不是重复提交（例如：日期不同，或名称匹配但日期为空且存在多条记录导致歧义）\"\n" +
                    "}\n" +
                    "```\n" +
                    "6. 输出时只返回这个 JSON 对象，**不要添加任何多余文字、解释或注释**。"
    )
    DeduplicationResult checkForDuplicate(
            @V("newAward") AwardInfo newAward,
            @V("oldAward") List<AwardSubmission> oldAward
    );

}