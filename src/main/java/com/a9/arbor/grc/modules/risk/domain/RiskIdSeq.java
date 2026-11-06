package com.a9.arbor.grc.modules.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "risk_id_seq")
@IdClass(RiskIdSeq.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class RiskIdSeq {

    @Id
    @Column(name = "risk_type", length = 16)
    private String riskType;

    @Id
    @Column(name = "year_num")
    private int yearNum;

    @Column(name = "next_val", nullable = false)
    private int nextVal;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private String riskType;
        private int yearNum;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk pk)) {
                return false;
            }
            return yearNum == pk.yearNum && java.util.Objects.equals(riskType, pk.riskType);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(riskType, yearNum);
        }
    }
}
