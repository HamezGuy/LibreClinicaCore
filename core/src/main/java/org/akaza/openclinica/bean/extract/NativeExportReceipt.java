package org.akaza.openclinica.bean.extract;

import java.net.URI;
import java.net.URISyntaxException;

/** API-owned selection receipts are not Java SQL extraction definitions.
 * Their native dataset/study IDs locate an authenticated EDC review; no source
 * body, clinical values, session token, or file path is forwarded. */
public final class NativeExportReceipt {
    private static final String PREFIX = "/* accura-export-selection-";

    private NativeExportReceipt() {
    }

    public static boolean isReceipt(String storedDefinition) {
        // Classification only. Never trim or reconstruct the stored SQL/body.
        return storedDefinition != null && storedDefinition.trim().startsWith(PREFIX);
    }

    public static void requireLegacyExtraction(String storedDefinition) {
        if (isReceipt(storedDefinition)) {
            throw new IllegalStateException("Native EDC export receipts require the authenticated EDC dataset review; Java SQL extraction is unavailable.");
        }
    }

    public static String reviewUrl(String configuredBaseUrl, int studyId, int datasetId) {
        if (studyId <= 0 || datasetId <= 0) {
            throw new IllegalArgumentException("Exact native study and dataset IDs are required.");
        }
        if (configuredBaseUrl == null || configuredBaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("The EDC export review UI has not been configured.");
        }
        try {
            URI base = new URI(configuredBaseUrl);
            if (!("https".equalsIgnoreCase(base.getScheme()) || "http".equalsIgnoreCase(base.getScheme()))
                    || base.getHost() == null || base.getRawUserInfo() != null
                    || base.getRawQuery() != null || base.getRawFragment() != null
                    || !base.normalize().equals(base)) {
                throw new IllegalArgumentException("The EDC export review UI must be an absolute HTTP(S) application URL without credentials, query, or fragment.");
            }
            String root = base.toASCIIString();
            while (root.endsWith("/")) {
                root = root.substring(0, root.length() - 1);
            }
            return root + "/studies/" + studyId + "/reports?exportDatasetId=" + datasetId;
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException("The EDC export review UI URL is invalid.", invalid);
        }
    }
}
