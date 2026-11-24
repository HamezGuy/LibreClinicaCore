package org.akaza.openclinica.logic.importdata;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.akaza.openclinica.bean.submit.crfdata.ImportItemDataBean;
import org.akaza.openclinica.exception.OpenClinicaException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bind an incoming ODM observation to the native unit and missing codes for its actual item and event form. */
public final class OdmItemImportValue {
    private OdmItemImportValue() {}

    public static String resolve(ImportItemDataBean item, int itemId, String allowedNulls, DataSource source)
            throws OpenClinicaException {
        String unitOid = item.getMeasurementUnitRef() == null ? null : item.getMeasurementUnitRef().getElementDefOID();
        if (unitOid != null) {
            // Matches the native ODM extractor's item.units = measurement_unit.name join.
            List<String> units = new JdbcTemplate(source).queryForList(
                    "SELECT mu.oc_oid FROM item i JOIN measurement_unit mu ON mu.name = i.units WHERE i.item_id = ?",
                    String.class, itemId);
            if (units.size() != 1 || !unitOid.equals(units.get(0))) {
                throw new OpenClinicaException("MeasurementUnitRef disagrees with the native unit for " + item.getItemOID(), "");
            }
        }
        String reason = item.getReasonForNull();
        List<String> nullCodes = allowedNulls == null || allowedNulls.isEmpty()
                ? java.util.Collections.emptyList() : Arrays.asList(allowedNulls.split(","));
        if (!"Yes".equals(item.getIsNull())) {
            if (reason != null || (item.getIsNull() != null && !"No".equals(item.getIsNull()))) {
                throw new OpenClinicaException("Invalid ODM missing-value declaration for " + item.getItemOID(), "");
            }
            if (item.getValue() != null && Arrays.stream(item.getValue().split(",")).map(String::trim).anyMatch(nullCodes::contains)) {
                throw new OpenClinicaException("A collected answer would be read as a native missing code for " + item.getItemOID(), "");
            }
            return item.getValue();
        }
        if (reason == null || reason.isEmpty() || (item.getValue() != null && !item.getValue().isEmpty())
                || !nullCodes.contains(reason)) {
            throw new OpenClinicaException("An explicit ODM null requires one native missing code allowed on this event form for " + item.getItemOID(), "");
        }
        return reason;
    }
}
