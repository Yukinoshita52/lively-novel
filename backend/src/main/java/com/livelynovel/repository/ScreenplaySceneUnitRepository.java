package com.livelynovel.repository;

import com.livelynovel.model.entity.ScreenplaySceneUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 切场产物仓储。
 */
public interface ScreenplaySceneUnitRepository extends JpaRepository<ScreenplaySceneUnitEntity, Long> {

    List<ScreenplaySceneUnitEntity> findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc(String conversionId);
}
