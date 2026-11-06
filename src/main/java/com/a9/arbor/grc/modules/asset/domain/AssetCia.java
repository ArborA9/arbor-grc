package com.a9.arbor.grc.modules.asset.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * CIA + register autos: locked to client Valuation Methodology /
 * Asset Register sheet rules ({@code Information_Asset_Register_ISO27001_UAE_IA.xlsx}).
 */
public final class AssetCia {

    private AssetCia() {
    }

    public static boolean isRated(Integer level) {
        return level != null && level >= 1 && level <= 5;
    }

    public static boolean allRated(Integer confidentiality, Integer integrity, Integer availability) {
        return isRated(confidentiality) && isRated(integrity) && isRated(availability);
    }

    /** Asset Value = MAX(C, I, A). Stored in {@code cia_score} for compatibility. */
    public static BigDecimal assetValue(int confidentiality, int integrity, int availability) {
        int max = Math.max(confidentiality, Math.max(integrity, availability));
        return BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_UP);
    }

    public static int aggregateScore(int confidentiality, int integrity, int availability) {
        return confidentiality + integrity + availability;
    }

    /** Weighted score with Wc=Wi=Wa=1 (tenant weights deferred). */
    public static BigDecimal weightedScore(int confidentiality, int integrity, int availability) {
        return BigDecimal.valueOf(confidentiality + integrity + availability)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }

    /**
     * Excel Tier map: 5 Critical, 4 High, 3 Medium, 2 Low, 1 Minimal (→ LOW).
     */
    public static AssetCriticality criticalityFromAssetValue(BigDecimal assetValue) {
        int v = assetValue.setScale(0, RoundingMode.HALF_UP).intValue();
        return switch (v) {
            case 5 -> AssetCriticality.CRITICAL;
            case 4 -> AssetCriticality.HIGH;
            case 3 -> AssetCriticality.MEDIUM;
            default -> AssetCriticality.LOW;
        };
    }

    /** Derived classification from Confidentiality (Valuation + Lists). */
    public static AssetClassification classificationFromConfidentiality(int confidentiality) {
        return switch (confidentiality) {
            case 5 -> AssetClassification.SECRET;
            case 4 -> AssetClassification.SENSITIVE;
            case 3, 2 -> AssetClassification.CONFIDENTIAL;
            default -> AssetClassification.OPEN;
        };
    }

    public static int classRank(AssetClassification classification) {
        if (classification == null) {
            return 0;
        }
        return switch (classification) {
            case SECRET -> 4;
            case SENSITIVE -> 3;
            case CONFIDENTIAL -> 2;
            case OPEN -> 1;
        };
    }

    public static AssetClassification finalClassification(AssetClassification derived,
                                                         AssetClassification applied) {
        return applied != null ? applied : derived;
    }

    public static String classificationCheck(AssetClassification derived,
                                             AssetClassification applied) {
        if (applied == null) {
            return "Derived from C rating";
        }
        if (classRank(applied) < classRank(derived)) {
            return "WARNING: owner override is lower than derived from C";
        }
        return "Owner override applied";
    }

    /** Recommended RTO hours from Availability (product defaults; Excel sample broken). */
    public static BigDecimal recommendedRtoHours(int availability) {
        return BigDecimal.valueOf(switch (availability) {
            case 5 -> 1;
            case 4 -> 4;
            case 3 -> 24;
            case 2 -> 72;
            default -> 168;
        });
    }

    /** Recommended RPO hours from Availability. */
    public static BigDecimal recommendedRpoHours(int availability) {
        return BigDecimal.valueOf(switch (availability) {
            case 5 -> 0;
            case 4 -> 1;
            case 3 -> 4;
            case 2 -> 24;
            default -> 168;
        });
    }

    public static String recoveryObjectiveCheck(BigDecimal recommendedRto,
                                                BigDecimal recommendedRpo,
                                                BigDecimal agreedRto,
                                                BigDecimal agreedRpo) {
        boolean weaker = false;
        if (agreedRto != null && recommendedRto != null && agreedRto.compareTo(recommendedRto) > 0) {
            weaker = true;
        }
        if (agreedRpo != null && recommendedRpo != null && agreedRpo.compareTo(recommendedRpo) > 0) {
            weaker = true;
        }
        if (agreedRto == null && agreedRpo == null) {
            return "OK";
        }
        return weaker
                ? "CHECK: agreed target weaker than the A rating implies"
                : "OK";
    }

    /** Review cycle months from Asset Value. */
    public static int reviewCycleMonths(BigDecimal assetValue) {
        int v = assetValue == null ? 1 : assetValue.setScale(0, RoundingMode.HALF_UP).intValue();
        return switch (v) {
            case 5 -> 3;
            case 4 -> 6;
            case 3 -> 12;
            case 2 -> 24;
            default -> 36;
        };
    }

    public static List<String> requiredControlBaseline(AssetClassification finalClass) {
        if (finalClass == null) {
            return List.of();
        }
        return switch (finalClass) {
            case SECRET, SENSITIVE -> List.of(
                    "Encrypted at Rest",
                    "Encrypted in Transit",
                    "MFA Enforced",
                    "Backed Up",
                    "Monitored by SIEM",
                    "Vulnerability Scanned");
            case CONFIDENTIAL -> List.of(
                    "Encrypted at Rest",
                    "Encrypted in Transit",
                    "Backed Up",
                    "Vulnerability Scanned");
            case OPEN -> List.of("Backed Up");
        };
    }

    public static String requiredControlBaselineText(AssetClassification finalClass) {
        List<String> items = requiredControlBaseline(finalClass);
        if (items.isEmpty()) {
            return null;
        }
        return String.join("; ", items);
    }

    public static List<String> baselineGaps(AssetClassification finalClass,
                                            AssetTriState encryptedAtRest,
                                            AssetTriState encryptedInTransit,
                                            AssetTriState mfaEnforced,
                                            AssetTriState backedUp,
                                            AssetTriState siemMonitored,
                                            AssetTriState vulnScanned) {
        List<String> expected = requiredControlBaseline(finalClass);
        List<String> gaps = new ArrayList<>();
        for (String control : expected) {
            AssetTriState state = switch (control) {
                case "Encrypted at Rest" -> encryptedAtRest;
                case "Encrypted in Transit" -> encryptedInTransit;
                case "MFA Enforced" -> mfaEnforced;
                case "Backed Up" -> backedUp;
                case "Monitored by SIEM" -> siemMonitored;
                case "Vulnerability Scanned" -> vulnScanned;
                default -> null;
            };
            if (state == null || state == AssetTriState.NO) {
                gaps.add(control);
            }
        }
        return gaps;
    }

    public static String baselineGapsText(List<String> gaps) {
        if (gaps == null || gaps.isEmpty()) {
            return "None";
        }
        return String.join("; ", gaps);
    }

    public static LocalDate nextReviewDue(LocalDate lastReviewed, int reviewCycleMonths) {
        if (lastReviewed == null) {
            return null;
        }
        return lastReviewed.plusMonths(reviewCycleMonths);
    }

    public static String reviewStatus(LocalDate nextReviewDue, LocalDate today) {
        if (nextReviewDue == null) {
            return "Unknown";
        }
        if (nextReviewDue.isBefore(today)) {
            return "Overdue";
        }
        return "On track";
    }

    public static String eolRiskFlag(LocalDate endOfLifeDate, LocalDate today) {
        if (endOfLifeDate == null) {
            return null;
        }
        if (endOfLifeDate.isBefore(today) || endOfLifeDate.isEqual(today)) {
            return "EOL PASSED " + endOfLifeDate;
        }
        long days = ChronoUnit.DAYS.between(today, endOfLifeDate);
        if (days <= 90) {
            return "Approaching EOL (" + days + " days)";
        }
        return "In support";
    }

    /** Human label for CIA scale 1–5. */
    public static String scaleLabel(int level) {
        return switch (level) {
            case 1 -> "1: Negligible / Minimal";
            case 2 -> "2: Low";
            case 3 -> "3: Medium";
            case 4 -> "4: High";
            case 5 -> "5: Critical / Catastrophic";
            default -> String.valueOf(level);
        };
    }

    /** Criticality display including Excel Tier 5 Minimal. */
    public static String criticalityLabel(AssetCriticality criticality, BigDecimal assetValue) {
        if (criticality == null) {
            return null;
        }
        int v = assetValue == null ? 0 : assetValue.setScale(0, RoundingMode.HALF_UP).intValue();
        if (v <= 1) {
            return "Tier 5: Minimal";
        }
        return switch (criticality) {
            case CRITICAL -> "Tier 1: Critical";
            case HIGH -> "Tier 2: High";
            case MEDIUM -> "Tier 3: Medium";
            case LOW -> "Tier 4: Low";
        };
    }
}
