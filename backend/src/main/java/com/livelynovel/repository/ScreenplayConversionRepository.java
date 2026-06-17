package com.livelynovel.repository;

import com.livelynovel.model.entity.ScreenplayConversionEntity;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 剧本转换任务仓储。
 */
public interface ScreenplayConversionRepository extends JpaRepository<ScreenplayConversionEntity, String> {

    Optional<ScreenplayConversionEntity> findFirstByNovelIdAndScreenplayTypeAndStatusOrderByUpdatedAtDesc(
            String novelId,
            ScreenplayTypeEnum screenplayType,
            String status
    );

    Optional<ScreenplayConversionEntity> findFirstByNovelIdAndScreenplayTypeAndStatusInOrderByUpdatedAtDesc(
            String novelId,
            ScreenplayTypeEnum screenplayType,
            List<String> statuses
    );

    List<ScreenplayConversionEntity> findByNovelIdInAndStatusInOrderByUpdatedAtDesc(
            List<String> novelIds,
            List<String> statuses
    );
}
