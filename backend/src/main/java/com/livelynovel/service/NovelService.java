package com.livelynovel.service;

import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.NovelListResultDTO;
import com.livelynovel.model.dto.NovelUploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小说落库与章节回读服务。
 */
public interface NovelService {

    NovelUploadResultDTO uploadTxt(String title, MultipartFile file);

    NovelChaptersResultDTO getChapters(String novelId);

    NovelListResultDTO listNovels();
}
