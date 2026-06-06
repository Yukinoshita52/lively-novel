package com.livelynovel.repository;

import com.livelynovel.model.entity.NovelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 小说仓储。
 */
public interface NovelRepository extends JpaRepository<NovelEntity, String> {
}
