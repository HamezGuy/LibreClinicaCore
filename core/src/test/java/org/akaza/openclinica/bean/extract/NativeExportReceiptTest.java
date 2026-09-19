package org.akaza.openclinica.bean.extract;

import junit.framework.TestCase;
import org.akaza.openclinica.service.extract.GenerateExtractFileService;

/** Pure contract checks: no Spring context, database, or extraction file. */
public class NativeExportReceiptTest extends TestCase {
    private static final String RECEIPT = "/* accura-export-selection-v1:eyJjb250cmFjdCI6ImVkYy1uYXRpdmUtZXhwb3J0LXNlbGVjdGlvbi8xIn0= */";

    public void testOrdinarySqlRemainsExactAndNamesDoNotClassifyDatasets() {
        DatasetBean dataset = new DatasetBean();
        String sql = "  SELECT value FROM item_data WHERE value = ''\nORDER BY item_data_id  ";
        dataset.setName("/* accura-export-selection-v1: a display label */");
        dataset.setSQLStatement(sql);
        assertFalse(dataset.isNativeExportReceipt());
        assertEquals(sql, dataset.getSQLStatement());
        dataset.setSQLStatement(null);
        assertNull(dataset.getSQLStatement());
    }

    public void testNativeReceiptCannotBeParsedOrExecutedAsSql() {
        DatasetBean dataset = new DatasetBean();
        dataset.setSQLStatement(RECEIPT);
        assertTrue(dataset.isNativeExportReceipt());
        try {
            dataset.getSQLStatement();
            fail("A native receipt must not reach Java SQL extraction");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("authenticated EDC dataset review"));
        }
        // Both scheduled extraction implementations enter this actual service.
        // The guard executes before any ExtractBean, DB, or output file work.
        GenerateExtractFileService service = new GenerateExtractFileService(null, null, null);
        try {
            service.generateExtractBean(dataset, null, null);
            fail("A scheduled native receipt must not generate legacy metadata");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Java SQL extraction is unavailable"));
        }
    }

    public void testCachedNativeDatasetCannotReplaceItsExactReceipt() {
        String original = "\r\n " + RECEIPT + "\n ";
        DatasetBean dataset = new DatasetBean();
        dataset.setSQLStatement(original);
        for (String replacement : new String[] { "SELECT value FROM item_data", null, "",
                RECEIPT, "/* accura-export-selection-v99:changed-body */" }) {
            try {
                dataset.setSQLStatement(replacement);
                fail("A cached native receipt must not be replaced or normalized by a legacy editor");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("authenticated EDC dataset review"));
            }
            assertTrue(dataset.isNativeExportReceipt());
            // Reassigning identical bytes remains valid; this also verifies
            // the refused edit left the original receipt in the bean.
            dataset.setSQLStatement(original);
        }
    }

    public void testCachedNativeDatasetCannotGenerateOrRewriteLegacySelection() {
        DatasetBean dataset = new DatasetBean();
        dataset.setSQLStatement(RECEIPT);
        try {
            // Actual CreateDatasetServlet specifysubmit sequence.
            dataset.setSQLStatement(dataset.generateQuery());
            fail("A native receipt must not be converted to a generated SQL selection");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Java SQL extraction is unavailable"));
        }
        try {
            // Actual confirmall/finalUpateDatasetBean sequence.
            dataset.setSQLStatement(dataset.sqlWithUniqeItemIds("item_id in (2, 3"));
            fail("A native receipt must not be parsed as a legacy item filter");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Java SQL extraction is unavailable"));
        }
        dataset.setSQLStatement(RECEIPT);
    }

    public void testOrdinarySelectionCanStillBeRewritten() {
        DatasetBean dataset = new DatasetBean();
        dataset.setSQLStatement("SELECT value FROM item_data WHERE item_id in (1, 2) ORDER BY item_id");
        dataset.setSQLStatement(dataset.sqlWithUniqeItemIds("item_id in (2, 3"));
        assertEquals("SELECT value FROM item_data WHERE item_id in (2, 3) ORDER BY item_id",
                dataset.getSQLStatement());
        dataset.setSQLStatement(dataset.generateQuery());
        assertFalse(dataset.isNativeExportReceipt());
        assertTrue(dataset.getSQLStatement().startsWith("select distinct * from "));
    }

    public void testUnknownReceiptVersionsAlsoCannotFallThroughToLegacySql() {
        for (String source : new String[] { RECEIPT, "\r\n " + RECEIPT, "/* accura-export-selection-v99:unrecognized */" }) {
            assertTrue(NativeExportReceipt.isReceipt(source));
            try {
                NativeExportReceipt.requireLegacyExtraction(source);
                fail("Receipt family must never execute as legacy SQL");
            } catch (IllegalStateException expected) {
                assertNotNull(expected.getMessage());
            }
        }
        assertFalse(NativeExportReceipt.isReceipt("SELECT 'accura-export-selection'"));
        assertFalse(NativeExportReceipt.isReceipt(""));
        assertFalse(NativeExportReceipt.isReceipt(null));
    }

    public void testReviewLinkContainsOnlyTheExactNativeLocators() {
        assertEquals("https://edc.example.invalid/studies/17/reports?exportDatasetId=913",
                NativeExportReceipt.reviewUrl("https://edc.example.invalid/", 17, 913));
        assertEquals("http://localhost:4200/edc/studies/17/reports?exportDatasetId=913",
                NativeExportReceipt.reviewUrl("http://localhost:4200/edc/", 17, 913));
    }

    public void testMissingOrUnsafeUiConfigurationIsRefused() {
        for (String base : new String[] { null, "", "/relative", "javascript:alert(1)",
                "https://user:token@edc.example.invalid", "https://edc.example.invalid/?autoExport=1",
                "https://edc.example.invalid/#token", "https://edc.example.invalid/a/../b", "https://edc.example.invalid/\r\nHeader: x" }) {
            try {
                NativeExportReceipt.reviewUrl(base, 17, 913);
                fail("Invalid UI configuration was accepted");
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    public void testMissingNativeIdsAreNotInventedFromLabels() {
        for (int[] ids : new int[][] { {0, 913}, {17, 0}, {-1, 913}, {17, -1} }) {
            try {
                NativeExportReceipt.reviewUrl("https://edc.example.invalid", ids[0], ids[1]);
                fail("Missing native identities were accepted");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("native study and dataset IDs"));
            }
        }
    }
}
