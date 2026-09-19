package org.akaza.openclinica.control.extract;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.akaza.openclinica.bean.extract.DatasetBean;
import org.akaza.openclinica.bean.extract.NativeExportReceipt;
import org.akaza.openclinica.dao.core.CoreResources;

/** Call only after the servlet's existing user/study/owner checks. EDC then
 * authenticates its own session and authorizes the dataset's actual native
 * owner again. A redirect is not a download or an activation approval. */
final class NativeDatasetReview {
    private NativeDatasetReview() {
    }

    static boolean redirect(DatasetBean dataset, HttpServletResponse response) throws IOException {
        if (!dataset.isNativeExportReceipt()) {
            return false;
        }
        return redirect(dataset, response, CoreResources.getField("edcExportUiBaseUrl"));
    }

    static boolean redirect(DatasetBean dataset, HttpServletResponse response, String configuredBaseUrl) throws IOException {
        if (!dataset.isNativeExportReceipt()) {
            return false;
        }
        final String location;
        try {
            location = NativeExportReceipt.reviewUrl(configuredBaseUrl, dataset.getStudyId(), dataset.getId());
        } catch (IllegalArgumentException invalid) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The native EDC export review UI is not configured correctly. No extraction was run.");
            return true;
        }
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.sendRedirect(location);
        return true;
    }
}
