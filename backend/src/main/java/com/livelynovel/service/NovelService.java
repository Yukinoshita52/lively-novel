package com.livelynovel.service;

import com.livelynovel.model.dto.NovelUploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小说上传落库服务。
 */
public interface NovelService {

    NovelUploadResultDTO uploadTxt(String title, MultipartFile file);
}
