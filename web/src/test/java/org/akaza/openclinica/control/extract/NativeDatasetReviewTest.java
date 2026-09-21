package org.akaza.openclinica.control.extract;

import javax.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import org.akaza.openclinica.bean.extract.DatasetBean;
import static org.mockito.Mockito.*;

/** Exercises the actual redirect boundary without starting a servlet, DB,
 * authentication provider, or extracting a file. Servlet authorization remains
 * before this helper; EDC performs its own live authorization after navigation. */
public class NativeDatasetReviewTest extends TestCase {
    private DatasetBean receipt() {
        DatasetBean dataset = new DatasetBean();
        dataset.setId(913);
        dataset.setStudyId(17);
        dataset.setName("A display label, not an identity");
        dataset.setSQLStatement("/* accura-export-selection-v1:retained-source-not-forwarded */");
        return dataset;
    }

    public void testRedirectCarriesOnlyNativeIdsAndNoAutomaticExport() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertTrue(NativeDatasetReview.redirect(receipt(), response, "https://edc.example.invalid"));
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response).setHeader("Referrer-Policy", "no-referrer");
        verify(response).sendRedirect("https://edc.example.invalid/studies/17/reports?exportDatasetId=913");
        verifyNoMoreInteractions(response);
    }

    public void testUnconfiguredUiFailsWithoutLegacyExtractionFallback() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertTrue(NativeDatasetReview.redirect(receipt(), response, ""));
        verify(response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "The native EDC export review UI is not configured correctly. No extraction was run.");
        verifyNoMoreInteractions(response);
    }

    public void testLegacyDatasetKeepsItsExistingServletFlow() throws Exception {
        DatasetBean dataset = new DatasetBean();
        dataset.setId(913);
        dataset.setStudyId(17);
        dataset.setSQLStatement("SELECT value FROM item_data");
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertFalse(NativeDatasetReview.redirect(dataset, response, ""));
        verifyZeroInteractions(response);
        assertEquals("SELECT value FROM item_data", dataset.getSQLStatement());
    }
}
