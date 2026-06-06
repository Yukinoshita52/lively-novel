package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 章节切分器（纯代码，确定性）。
 *
 * <p>对应 {@code docs/technical-design.md §5.2 步骤①「章节分割（纯代码，确定性）」}——
 * 不调用 LLM。面向 {@code ===} 围栏结构的小说（典型为日式轻小说）：
 *
 * <pre>
 * ==================================================   ← 开分隔线
 * ~一败目~                                              ← 标题块（首行波浪号包围 + 0..N 行副标题）
 * 专业青梅竹马
 * ==================================================   ← 闭分隔线
 *
 * 正文……                                               ← 正文，直到下一章开分隔线
 * </pre>
 *
 * <p>仅「标题块首行匹配章节判定式」者算章节；书名 / 制作信息 / 简介 / Intermission /
 * 后记 等同样被 {@code ===} 包围但标题非波浪号包围，会被自动排除，其正文折入前一个章节尾部。
 *
 * <p>判定式可替换：默认 {@code ~...~}（{@link #DEFAULT_CHAPTER_TITLE}）。未来支持
 * {@code 第X章} / {@code Chapter X} 只需传入新的判定式，主流程不变。
 */
@Service
public class ChapterSplitter {

    /** 分隔线：整行 5 个及以上等号（制作信息用的 ≡(U+2261) 是别的字符，不会误中）。 */
    private static final Pattern SEPARATOR = Pattern.compile("^={5,}$");

    /** 默认章节标题判定式：波浪号包围，如 {@code ~一败目~}。 */
    public static final Predicate<String> DEFAULT_CHAPTER_TITLE =
            line -> line.matches("^~.+~$");

    /** 标题块最大非空行数（正文块都远超此值，留有充足余量）。 */
    private static final int MAX_TITLE_LINES = 6;

    private final Predicate<String> isChapterTitle;

    public ChapterSplitter() {
        this(DEFAULT_CHAPTER_TITLE);
    }

    public ChapterSplitter(Predicate<String> isChapterTitle) {
        this.isChapterTitle = isChapterTitle;
    }

    /**
     * 将原始小说文本切分为章节列表。
     *
     * @param text 原始小说文本
     * @return 章节列表（顺序、序号从 1 开始）
     * @throws IllegalArgumentException 文本为空或全空白
     */
    public List<ChapterDTO> split(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        String[] lines = text.split("\\r?\\n", -1);
        List<Integer> separators = findSeparatorLines(lines);
        List<Heading> chapterHeadings = findChapterHeadings(lines, separators);

        return extractChapters(lines, chapterHeadings);
    }

    /** 定位所有分隔线的行号。 */
    private List<Integer> findSeparatorLines(String[] lines) {
        List<Integer> separators = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (SEPARATOR.matcher(lines[i].strip()).matches()) {
                separators.add(i);
            }
        }
        return separators;
    }

    /**
     * 在「相邻分隔线之间、非空行数 1..MAX_TITLE_LINES」的标题块中，
     * 挑出首行匹配章节判定式的块作为章节标题。
     */
    private List<Heading> findChapterHeadings(String[] lines, List<Integer> separators) {
        List<Heading> headings = new ArrayList<>();
        for (int i = 0; i + 1 < separators.size(); i++) {
            int open = separators.get(i);
            int close = separators.get(i + 1);

            List<String> titleLines = new ArrayList<>();
            for (int k = open + 1; k < close; k++) {
                String stripped = lines[k].strip();
                if (!stripped.isEmpty()) {
                    titleLines.add(stripped);
                }
            }

            if (titleLines.isEmpty() || titleLines.size() > MAX_TITLE_LINES) {
                continue; // 空块或正文块，不是标题块
            }
            if (!isChapterTitle.test(titleLines.get(0))) {
                continue; // 标题块但非章节（书名 / 简介 / Intermission / 后记 等）
            }
            headings.add(new Heading(open, close, buildTitle(titleLines)));
        }
        return headings;
    }

    /** 标题块各行去掉装饰符（波浪号）与空白后用空格拼接。 */
    private String buildTitle(List<String> titleLines) {
        List<String> cleaned = new ArrayList<>();
        for (String line : titleLines) {
            cleaned.add(line.replaceAll("^[~\\s]+|[~\\s]+$", ""));
        }
        return String.join(" ", cleaned);
    }

    /**
     * 按章节标题位置提取正文。每章正文 = 本章闭合分隔线之后 →
     * 下一章起始分隔线之前（或文末）。Intermission / 后记 自然折入前一章。
     */
    private List<ChapterDTO> extractChapters(String[] lines, List<Heading> headings) {
        List<ChapterDTO> chapters = new ArrayList<>();
        for (int i = 0; i < headings.size(); i++) {
            int contentStart = headings.get(i).closeFence + 1;
            int contentEnd = (i + 1 < headings.size())
                    ? headings.get(i + 1).openFence
                    : lines.length;

            String content = joinLines(lines, contentStart, contentEnd).strip();

            ChapterDTO chapter = new ChapterDTO();
            chapter.setChapterIndex(i + 1);
            chapter.setTitle(headings.get(i).title);
            chapter.setContent(content);
            chapter.setWordCount(countWords(content));
            chapters.add(chapter);
        }
        return chapters;
    }

    private String joinLines(String[] lines, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int k = from; k < to; k++) {
            if (k > from) {
                sb.append('\n');
            }
            sb.append(lines[k]);
        }
        return sb.toString();
    }

    /** 字数：去掉所有空白字符后的字符数。 */
    private int countWords(String content) {
        return content.replaceAll("\\s+", "").length();
    }

    /** 一个章节标题块的位置与标题。 */
    private static final class Heading {
        final int openFence;
        final int closeFence;
        final String title;

        Heading(int openFence, int closeFence, String title) {
            this.openFence = openFence;
            this.closeFence = closeFence;
            this.title = title;
        }
    }
}
