package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterDTO;
import com.livelynovel.service.ChapterSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 章节切分器（纯代码，确定性）。
 *
 * <p>对应 {@code docs/technical-design.md §5.2 步骤①「章节分割（纯代码，确定性）」}——
 * 不调用 LLM。面向 {@code ===} 围栏结构的小说（典型为日式轻小说）。
 */
@Service
public class ChapterSplitterImpl implements ChapterSplitter {

    /** 分隔线：整行 5 个及以上等号（制作信息用的 ≡(U+2261) 是别的字符，不会误中）。 */
    private static final Pattern SEPARATOR = Pattern.compile("^={5,}$");

    /** 默认章节标题判定式：波浪号包围，如 {@code ~一败目~}。 */
    public static final Predicate<String> DEFAULT_CHAPTER_TITLE =
            line -> line.matches("^~.+~$");

    /** 标题块最大非空行数（正文块都远超此值，留有充足余量）。 */
    private static final int MAX_TITLE_LINES = 6;

    private final Predicate<String> isChapterTitle;

    public ChapterSplitterImpl() {
        this(DEFAULT_CHAPTER_TITLE);
    }

    public ChapterSplitterImpl(Predicate<String> isChapterTitle) {
        this.isChapterTitle = isChapterTitle;
    }

    @Override
    public List<ChapterDTO> split(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        String[] lines = text.split("\\r?\\n", -1);
        List<Integer> separators = findSeparatorLines(lines);
        List<Heading> chapterHeadings = findChapterHeadings(lines, separators);

        return extractChapters(lines, chapterHeadings);
    }

    private List<Integer> findSeparatorLines(String[] lines) {
        List<Integer> separators = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (SEPARATOR.matcher(lines[i].strip()).matches()) {
                separators.add(i);
            }
        }
        return separators;
    }

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
                continue;
            }
            if (!isChapterTitle.test(titleLines.get(0))) {
                continue;
            }
            headings.add(new Heading(open, close, buildTitle(titleLines)));
        }
        return headings;
    }

    private String buildTitle(List<String> titleLines) {
        List<String> cleaned = new ArrayList<>();
        for (String line : titleLines) {
            cleaned.add(line.replaceAll("^[~\\s]+|[~\\s]+$", ""));
        }
        return String.join(" ", cleaned);
    }

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

    private int countWords(String content) {
        return content.replaceAll("\\s+", "").length();
    }

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
