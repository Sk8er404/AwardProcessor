package org.com.code.certificateProcessor.util.validator;

import lombok.Getter;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 专门用于校验和清洗 AI OCR 服务返回的日期字符串的工具类。
 * 旨在处理 AI 可能返回的各种不规范或错误格式。
 */
public class AIResponseDateValidator {

    // 匹配 YYYY-MM-DD
    public static final Pattern PATTERN_YYYY_MM_DD = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
    // 匹配 YYYY-MM
    public static final Pattern PATTERN_YYYY_MM = Pattern.compile("^(\\d{4})-(\\d{1,2})$");
    // 匹配 YYYY
    public static final Pattern PATTERN_YYYY = Pattern.compile("^(\\d{4})$");

    /**
     * 尝试解析AI返回的日期字符串，并清理常见的AI错误 (如 00 月, 00 日)。
     * * @param dateStr AI返回的日期字符串，可能是 "2000-00-00", "2025-07-00", "2025-07", "2025", null 等。
     * @return 一个合法的 LocalDate 对象；如果无法解析或输入为 null/空，则返回 null。
     */
    public static LocalDate parseAndCleanDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // 预处理。比如，修复 "2000-00-00" 错误
        // 将所有非法的 "00" 月份或 "00" 日期替换为 "01"。
        String cleanedStr = dateStr.replace("-00", "-01");

        Matcher matcher;

        // 场景 1: YYYY-MM-DD
        matcher = PATTERN_YYYY_MM_DD.matcher(cleanedStr);
        if (matcher.matches()) {
            try {
                // 尝试直接解析。
                return LocalDate.parse(cleanedStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                // 即使正则匹配，也可能是非法日期 (如 "2025-02-30")
                return null;
            }
        }

        // 场景 2: YYYY-MM (AI 未按 OCRService 提示补全 "日")
        // 对应在 OCRService 中的规则
        matcher = PATTERN_YYYY_MM.matcher(cleanedStr);
        if (matcher.matches()) {
            try {
                // 默认为该月1号
                return LocalDate.parse(cleanedStr + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                return null; // 月份非法 (如 "2025-13")
            }
        }

        // 场景 3: YYYY (AI 未按 OCRService 提示补全 "月" 和 "日")
        // 对应您在 OCRService 中的规则
        matcher = PATTERN_YYYY.matcher(cleanedStr);
        if (matcher.matches()) {
            try {
                int year = Integer.parseInt(cleanedStr);
                // 校验年份是否在一个合理区间 (当前年份-5 到 当前年份)
                if (year >(Year.now().getValue()-5) && year <= Year.now().getValue()) {
                    // 默认为该年1月1号
                    return LocalDate.of(year, 1, 1);
                } else {
                    return null; // 年份不合理
                }
            } catch (Exception e) {
                return null; // 数字转换失败等
            }
        }

        // 场景 4: 格式完全不匹配 (如 "2000年", "未知", "25-02-2023" 等)
        return null;
    }
}