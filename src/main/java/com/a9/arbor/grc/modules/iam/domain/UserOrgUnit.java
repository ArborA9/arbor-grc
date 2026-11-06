package com.a9.arbor.grc.modules.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_org_unit")
@IdClass(UserOrgUnit.Pk.class)
@Getter
@Setter
public class UserOrgUnit {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long userId;
        private Long orgUnitId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(userId, pk.userId) && Objects.equals(orgUnitId, pk.orgUnitId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, orgUnitId);
        }
    }
}
