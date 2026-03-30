package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.static_.entity.StaticVacancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaticVacancyRepository extends JpaRepository<StaticVacancy, UUID> {

    List<StaticVacancy> findByStaticGroupIdAndIsOpenTrue(UUID staticId);
}
