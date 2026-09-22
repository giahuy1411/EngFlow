package com.datn.engflow.model.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for audit-v13 F-13-07.
 *
 * <p>The live DB had NO singleton CHECK on {@code study_policy} because the constraint was
 * declared inside a table-existence guard in {@code deploy.sql} that Hibernate's
 * {@code ddl-auto=update} always skipped (measured 2026-09-22:
 * {@code sys.check_constraints} on study_policy = 0). The same bug class as audit-v10 F124.
 *
 * <p>These assertions pin the entity-side declaration so it cannot be dropped silently.
 * The live DB is patched separately by {@code tasks/streak-study/fix-study-policy-check.sql}
 * because {@code ddl-auto=update} does not add constraints to existing tables.
 */
class StudyPolicySingletonGuardTest {

    @Test
    void entityDeclaresSingletonCheckConstraint() {
        Table table = StudyPolicy.class.getAnnotation(Table.class);
        assertThat(table).as("StudyPolicy must be mapped with @Table").isNotNull();

        CheckConstraint[] checks = table.check();
        assertThat(checks)
                .as("study_policy must declare a CHECK constraint on the entity")
                .isNotEmpty();

        assertThat(checks)
                .anySatisfy(c -> {
                    assertThat(c.name()).isEqualTo("ck_study_policy_singleton");
                    assertThat(c.constraint()).isEqualTo("id = 1");
                });
    }
}
