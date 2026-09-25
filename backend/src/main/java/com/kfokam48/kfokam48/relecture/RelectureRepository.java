package com.kfokam48.kfokam48.relecture;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<RelectureEntity, Long> {

    Optional<RelectureEntity> findByExerciceId(Long exerciceId);
}
