package com.livelynovel.repository;

import com.livelynovel.model.entity.ScreenplayConversionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 剧本转换任务仓储。
 */
public interface ScreenplayConversionRepository extends JpaRepository<ScreenplayConversionEntity, String> {
}
