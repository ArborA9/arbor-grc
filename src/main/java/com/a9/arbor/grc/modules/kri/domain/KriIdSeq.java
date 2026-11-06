package com.a9.arbor.grc.modules.kri.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "kri_id_seq")
@IdClass(KriIdSeq.Pk.class)
@Getter
@Setter
public class KriIdSeq {

    @Id
    @Column(name = "year_val", nullable = false)
    private Integer yearVal;

    @Column(name = "next_val", nullable = false)
    private Integer nextVal;

    public static class Pk implements Serializable {
        private Integer yearVal;

        public Pk() {
        }

        public Pk(Integer yearVal) {
            this.yearVal = yearVal;
        }

        public Integer getYearVal() {
            return yearVal;
        }

        public void setYearVal(Integer yearVal) {
            this.yearVal = yearVal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(yearVal, pk.yearVal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(yearVal);
        }
    }
}
