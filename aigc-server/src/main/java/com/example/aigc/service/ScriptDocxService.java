package com.example.aigc.service;

import com.example.aigc.exception.BizException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ScriptDocxService {

    public String extractText(String fileName, byte[] bytes) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".docx")) {
            return extractDocx(bytes);
        }
        throw new BizException(400, "仅支持上传 .txt / .md / .docx 文件");
    }

    private String extractDocx(byte[] bytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder builder = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append("\n\n");
                    }
                    builder.append(text.trim());
                }
            });
            return builder.toString();
        } catch (IOException ex) {
            throw new BizException(400, "DOCX 解析失败，请检查文件内容");
        }
    }
}
