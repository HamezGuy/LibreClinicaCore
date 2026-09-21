package org.akaza.openclinica.controller;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import junit.framework.TestCase;
import org.akaza.openclinica.bean.core.Role;
import org.akaza.openclinica.bean.extract.DatasetBean;
import org.akaza.openclinica.bean.login.StudyUserRoleBean;
import org.akaza.openclinica.i18n.util.ResourceBundleProvider;
import static org.mockito.Mockito.*;

/** Calls the actual Spring controller with only its native dataset read
 * supplied by a fixture. No scheduler, DB, export properties or files exist. */
public class NativeExtractControllerTest extends TestCase {
    private static final String SOURCE = "/* accura-export-selection-v1:exact-source */";
    private Locale previousLocale;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        previousLocale = ResourceBundleProvider.getLocale();
        ResourceBundleProvider.updateLocale(Locale.ENGLISH);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (previousLocale == null) {
                ResourceBundleProvider.localeMap.remove(Thread.currentThread());
            } else {
                ResourceBundleProvider.updateLocale(previousLocale);
            }
        } finally {
            super.tearDown();
        }
    }

    private static final class FixtureController extends ExtractController {
        final DatasetBean dataset = new DatasetBean();
        int reads;

        FixtureController() {
            dataset.setId(913);
            dataset.setStudyId(17);
            dataset.setName("Retained native name");
            dataset.setSQLStatement(SOURCE);
        }

        @Override
        protected DatasetBean findDataset(int datasetId) {
            assertEquals(913, datasetId);
            reads++;
            return dataset;
        }
    }

    private HttpServletRequest request(Role role) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        StudyUserRoleBean userRole = new StudyUserRoleBean();
        userRole.setRole(role);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/libreclinica");
        when(session.getAttribute("userRole")).thenReturn(userRole);
        return request;
    }

    public void testNativeReceiptReachesExistingScopedReviewBeforeJobPreparation() throws Exception {
        FixtureController controller = new FixtureController();
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertNull(controller.processSubmit("no-legacy-format-configured", "913", request(Role.COORDINATOR), response));
        assertEquals(1, controller.reads);
        assertEquals("Retained native name", controller.dataset.getName());
        controller.dataset.setSQLStatement(SOURCE);
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response).setHeader("Referrer-Policy", "no-referrer");
        verify(response).sendRedirect("/libreclinica/ExportDataset?datasetId=913");
        verifyNoMoreInteractions(response);
    }

    public void testExistingRoleRefusalOccursBeforeDatasetLookup() throws Exception {
        FixtureController controller = new FixtureController();
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertNull(controller.processSubmit("1", "913", request(Role.RESEARCHASSISTANT), response));
        assertEquals(0, controller.reads);
        verify(response).sendRedirect("/libreclinica/MainMenu?message=authentication_failed");
        verifyNoMoreInteractions(response);
    }

    public void testFailedNavigationCannotFallThroughToScheduling() throws Exception {
        FixtureController controller = new FixtureController();
        HttpServletResponse response = mock(HttpServletResponse.class);
        IOException failure = new IOException("Fixture response closed");
        doThrow(failure).when(response).sendRedirect("/libreclinica/ExportDataset?datasetId=913");
        try {
            controller.processSubmit("no-legacy-format-configured", "913", request(Role.MONITOR), response);
            fail("Navigation failure must not begin a legacy extraction");
        } catch (IOException expected) {
            assertSame(failure, expected);
        }
        assertEquals(1, controller.reads);
        controller.dataset.setSQLStatement(SOURCE);
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response).setHeader("Referrer-Policy", "no-referrer");
        verify(response).sendRedirect("/libreclinica/ExportDataset?datasetId=913");
        verifyNoMoreInteractions(response);
    }
}
