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

/** Modules an Admin may use. Ignored when the user is also an Owner. */
@Entity
@Table(name = "user_admin_module")
@IdClass(UserAdminModule.Pk.class)
@Getter
@Setter
public class UserAdminModule {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "module_code", nullable = false, length = 32)
    private String moduleCode;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long userId;
        private String moduleCode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(userId, pk.userId) && Objects.equals(moduleCode, pk.moduleCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, moduleCode);
        }
    }
}
