package com.rolemate.backend.persistence.repository;

import com.rolemate.backend.persistence.entity.FeedbackRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for session feedback records.
 */
@Repository
public interface FeedbackRecordRepository extends CrudRepository<FeedbackRecord, Long> {
}
