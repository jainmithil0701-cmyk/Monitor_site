package com.siteguard.repo;

import com.siteguard.model.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    Optional<CheckResult> findFirstBySiteIdOrderByCheckedAtDesc(Long siteId);

    List<CheckResult> findTop20BySiteIdOrderByCheckedAtDesc(Long siteId);

    long countBySiteId(Long siteId);

    long countBySiteIdAndSuccessTrue(Long siteId);

    void deleteBySiteId(Long siteId);
}
