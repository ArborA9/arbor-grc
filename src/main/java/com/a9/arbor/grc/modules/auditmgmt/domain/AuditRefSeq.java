package com.a9.arbor.grc.modules.auditmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "audit_ref_seq")
@Getter
@Setter
public class AuditRefSeq {

    @EmbeddedId
    private Pk id;

    @Column(name = "next_val", nullable = false)
    private int nextVal;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        @Column(nullable = false, length = 16)
        private String kind;
        @Column(name = "year_num", nullable = false)
        private int yearNum;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk pk)) {
                return false;
            }
            return yearNum == pk.yearNum && Objects.equals(kind, pk.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, yearNum);
        }
    }
}
