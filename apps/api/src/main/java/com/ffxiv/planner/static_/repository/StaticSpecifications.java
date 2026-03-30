package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.auth.entity.Role;
import com.ffxiv.planner.static_.entity.StaticGroup;
import com.ffxiv.planner.static_.entity.StaticStatus;
import org.springframework.data.jpa.domain.Specification;

public final class StaticSpecifications {

    private StaticSpecifications() {}

    public static Specification<StaticGroup> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<StaticGroup> hasDataCenter(String dataCenter) {
        return (root, query, cb) -> dataCenter == null ? cb.conjunction()
                : cb.equal(root.get("dataCenter"), dataCenter);
    }

    public static Specification<StaticGroup> hasStatus(StaticStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<StaticGroup> hasLanguage(String language) {
        return (root, query, cb) -> language == null ? cb.conjunction()
                : cb.equal(root.get("language"), language);
    }

    public static Specification<StaticGroup> nameContains(String query) {
        return (root, q, cb) -> query == null || query.isBlank() ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%");
    }

    public static Specification<StaticGroup> hasRoleNeeded(Role role) {
        return (root, query, cb) -> {
            if (role == null) return cb.conjunction();
            var vacanciesJoin = root.join("vacancies");
            return cb.and(
                    cb.equal(vacanciesJoin.get("roleNeeded"), role),
                    cb.isTrue(vacanciesJoin.get("isOpen"))
            );
        };
    }
}
