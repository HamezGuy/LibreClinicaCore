package org.akaza.openclinica.logic.importdata;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import javax.sql.DataSource;
import org.akaza.openclinica.bean.submit.crfdata.ImportItemDataBean;
import org.akaza.openclinica.bean.submit.crfdata.ODMContainer;
import org.akaza.openclinica.exception.OpenClinicaException;
import org.akaza.openclinica.logic.odmExport.ClinicalDataUtil;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OdmItemImportValueTest {
    private ImportItemDataBean parse(String attributes, String child) throws Exception {
        String xml = "<ODM xmlns='http://www.cdisc.org/ns/odm/v1.3' xmlns:oc='http://www.openclinica.org/ns/odm_ext_v130/v3.1'>"
                + "<ClinicalData StudyOID='S'><SubjectData SubjectKey='P'><StudyEventData StudyEventOID='V'>"
                + "<FormData FormOID='F'><ItemGroupData ItemGroupOID='G'><ItemData ItemOID='I' "
                + attributes + ">" + child + "</ItemData></ItemGroupData></FormData></StudyEventData></SubjectData></ClinicalData></ODM>";
        Mapping mapping = new Mapping();
        mapping.loadMapping(getClass().getResource("/properties/cd_odm_mapping.xml"));
        ODMContainer data = (ODMContainer) new Unmarshaller(mapping).unmarshal(new StringReader(xml));
        return data.getCrfDataPostImportContainer().getSubjectData().get(0).getStudyEventData().get(0)
                .getFormData().get(0).getItemGroupData().get(0).getItemData().get(0);
    }

    @Test public void missingReasonRoundTripsThroughTheNativeMissingCode() throws Exception {
        ImportItemDataBean source = parse("IsNull='Yes' Value='' oc:ReasonForNull='NA'", "");
        String stored = OdmItemImportValue.resolve(source, 12, "UNK,NA", null);
        assertEquals("NA", stored);
        HashMap<String,String> profile = new HashMap<>();
        profile.put("V.F", "UNK,NA");
        assertTrue(ClinicalDataUtil.isNull(stored, "V.F", profile));
        assertEquals("NA", ClinicalDataUtil.getNullsInValue(stored, ClinicalDataUtil.presetNullValueStr("UNK,NA")));
        try {
            OdmItemImportValue.resolve(source, 12, "UNK", null);
            fail("a reason outside this form's missing vocabulary must be refused");
        } catch (OpenClinicaException expected) { }
    }

    @Test public void refusesCollectedTextThatTheNativeReaderWouldReclassifyAsMissing() throws Exception {
        try {
            OdmItemImportValue.resolve(parse("Value='NA'", ""), 12, "NA,UNK", null);
            fail("the clinical value must survive native readback as a collected answer");
        } catch (OpenClinicaException expected) { }
        assertEquals("NA", OdmItemImportValue.resolve(parse("Value='NA'", ""), 12, "UNK", null));
    }

    @Test public void verifiesTheIncomingUnitAgainstTheExactNativeItem() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(ds.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(1);
        when(rows.next()).thenReturn(true, false);
        when(rows.getString(1)).thenReturn("MU_KG");
        ImportItemDataBean item = parse("Value='001'", "<MeasurementUnitRef MeasurementUnitOID='MU_KG'/>");
        assertEquals("MU_KG", item.getMeasurementUnitRef().getElementDefOID());
        assertEquals("001", OdmItemImportValue.resolve(item, 12, null, ds));
        when(rows.next()).thenReturn(true, false);
        when(rows.getString(1)).thenReturn("MU_LB");
        try {
            OdmItemImportValue.resolve(item, 12, null, ds);
            fail("a unit from a different item must be refused");
        } catch (OpenClinicaException expected) { }
    }
}
