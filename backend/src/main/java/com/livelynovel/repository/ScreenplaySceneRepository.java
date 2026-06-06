package com.livelynovel.repository;

import com.livelynovel.model.entity.ScreenplaySceneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 单场剧本产物仓储。
 */
public interface ScreenplaySceneRepository extends JpaRepository<ScreenplaySceneEntity, Long> {

    List<ScreenplaySceneEntity> findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc(String conversionId);
}
