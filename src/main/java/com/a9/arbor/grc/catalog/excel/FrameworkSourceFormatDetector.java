package com.a9.arbor.grc.catalog.excel;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Picks the parser for an uploaded framework workbook. Sits in front of
 * {@code FrameworkLibraryService.importExcel}, which previously branched on
 * {@code isChecklistWorkbook} alone.
 */
@Component
public class FrameworkSourceFormatDetector {

    private final MasterImportParser masterImportParser;
    private final GrcChecklistExcelParser checklistExcelParser;
    private final PciDssPackParser pciDssPackParser;
    private final Nist80053PackParser nist80053PackParser;
    private final ReferencePackParser referencePackParser;

    public FrameworkSourceFormatDetector(MasterImportParser masterImportParser,
                                         GrcChecklistExcelParser checklistExcelParser,
                                         PciDssPackParser pciDssPackParser,
                                         Nist80053PackParser nist80053PackParser,
                                         ReferencePackParser referencePackParser) {
        this.masterImportParser = masterImportParser;
        this.checklistExcelParser = checklistExcelParser;
        this.pciDssPackParser = pciDssPackParser;
        this.nist80053PackParser = nist80053PackParser;
        this.referencePackParser = referencePackParser;
    }

    /**
     * Order matters. Master Import goes first because it is identified by header shape rather than
     * sheet name, so it cannot be confused with anything else. GRC Checklist goes ahead of the
     * reference packs because it is pinned to four exact sheet names, which is the narrower test.
     * PCI DSS (S2) and NIST 800-53 (S3) are checked before the S4 reference packs because they carry
     * a single {@code Gap Assessment Question} column rather than a {@code Q1..Qn} bank. Anything
     * unrecognised still falls through to {@link FrameworkSourceFormat#ARBOR_CONTROLS} and its
     * missing-{@code Controls}-sheet error.
     */
    public FrameworkSourceFormat detect(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            if (masterImportParser.isMasterWorkbook(workbook)) {
                return FrameworkSourceFormat.MASTER_IMPORT;
            }
            if (checklistExcelParser.isChecklistWorkbook(workbook)) {
                return FrameworkSourceFormat.GRC_CHECKLIST;
            }
            if (pciDssPackParser.isPciDssPack(workbook)) {
                return FrameworkSourceFormat.PCI_DSS_PACK;
            }
            if (nist80053PackParser.isNist80053Pack(workbook)) {
                return FrameworkSourceFormat.NIST_800_53_PACK;
            }
            if (referencePackParser.isReferencePack(workbook)) {
                return FrameworkSourceFormat.REFERENCE_PACK;
            }
            return FrameworkSourceFormat.ARBOR_CONTROLS;
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    private static Workbook open(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return new XSSFWorkbook(in);
        }
    }
}
