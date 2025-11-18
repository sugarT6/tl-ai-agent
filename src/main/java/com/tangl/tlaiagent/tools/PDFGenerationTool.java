package com.tangl.tlaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.tangl.tlaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;


public class PDFGenerationTool {

    @Tool(description = "Generate a well-formatted PDF file with proper styling",returnDirect = true)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;

        if (!fileName.toLowerCase().endsWith(".pdf")) {
            filePath += ".pdf";
        }

        try {
            FileUtil.mkdir(fileDir);
            String cleanedContent = cleanContent(content);

            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                PdfFont font = createSafeFont();
                document.setFont(font);

                // 设置文档边距
                document.setMargins(40, 40, 40, 40);

                // 添加文档标题
                addDocumentTitle(document, "七夕约会计划");

                // 解析并添加内容
                addFormattedContent(document, cleanedContent);
            }
            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private void addDocumentTitle(Document document, String title) {
        Paragraph titlePara = new Paragraph(title)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f);
        document.add(titlePara);
    }

    private void addFormattedContent(Document document, String content) {
        String[] sections = content.split("##");

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            String[] lines = section.split("\n");
            if (lines.length == 0) continue;

            // 处理章节标题
            String sectionTitle = lines[0].trim();
            if (!sectionTitle.isEmpty()) {
                Paragraph sectionHeader = new Paragraph(sectionTitle)
                        .setFontSize(16)
                        .setMarginTop(15f)
                        .setMarginBottom(10f)
                        .setUnderline();
                document.add(sectionHeader);
            }

            // 处理章节内容
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("-")) {
                    // 列表项
                    addListItem(document, line.substring(1).trim());
                } else if (line.startsWith(">")) {
                    // 备注/引用
                    addNoteItem(document, line.substring(1).trim());
                } else if (line.contains(":**")) {
                    // 键值对格式（如：**时间**:晚上6:30）
                    addKeyValueItem(document, line);
                } else {
                    // 普通段落
                    addParagraph(document, line);
                }
            }

            // 添加章节间隔
            document.add(new Paragraph(" ").setMarginBottom(10f));
        }
    }

    private void addListItem(Document document, String text) {
        List list = new List()
                .setSymbolIndent(20)
                .setListSymbol("•")
                .setFontSize(12)
                .setMarginBottom(5f);

        ListItem item = new ListItem(text);
        list.add(item);
        document.add(list);
    }

    private void addNoteItem(Document document, String text) {
        Paragraph note = new Paragraph("备注: " + text)
                .setFontSize(11)
                .setMargin(8f)
                .setPadding(8f)
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(1f))
                .setMarginBottom(10f);
        document.add(note);
    }

    private void addKeyValueItem(Document document, String line) {
        // 处理 **键**:值 格式
        String[] parts = line.split(":**", 2);
        if (parts.length == 2) {
            String key = parts[0].replace("", "").trim() + ":";
            String value = parts[1].trim();

            Paragraph kvParagraph = new Paragraph()
                    .setFontSize(12)
                    .setMarginBottom(5f);

            kvParagraph.add(new Paragraph(key));
            kvParagraph.add(" " + value);

            document.add(kvParagraph);
        }
    }

    private void addParagraph(Document document, String text) {
        Paragraph paragraph = new Paragraph(text)
                .setFontSize(12)
                .setMarginBottom(8f)
                .setTextAlignment(TextAlignment.LEFT);
        document.add(paragraph);
    }

    private String cleanContent(String content) {
        if (content == null) return "";

        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (Character.isBmpCodePoint(c)) {
                cleaned.append(c);
            } else {
                cleaned.append(' ');
            }
        }
        return cleaned.toString();
    }

    private PdfFont createSafeFont() throws IOException {
        try {
            return PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
            } catch (Exception e2) {
                return PdfFontFactory.createFont();
            }
        }
    }
}