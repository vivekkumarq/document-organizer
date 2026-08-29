package com.vivek.docorganizer.repository;

import com.vivek.docorganizer.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findByUserId(Long userId);

    long countByUserId(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    Optional<Document> findByUserIdAndChecksumSha256(Long userId, String checksumSha256);

    @Query("select coalesce(sum(d.sizeBytes), 0) from Document d where d.userId = :userId")
    long sumSizeBytesByUserId(@Param("userId") Long userId);

    /** Per-content-type breakdown: [contentType, fileCount, bytesUsed]. */
    @Query("""
            select d.contentType, count(d), coalesce(sum(d.sizeBytes), 0)
            from Document d
            where d.userId = :userId
            group by d.contentType
            order by sum(d.sizeBytes) desc
            """)
    List<Object[]> storageBreakdownByUserId(@Param("userId") Long userId);

    /** Distinct tags in use by one user, for the filter dropdown in the UI. */
    @Query("select distinct t from Document d join d.tags t where d.userId = :userId order by t")
    List<String> findDistinctTagsByUserId(@Param("userId") Long userId);
}
