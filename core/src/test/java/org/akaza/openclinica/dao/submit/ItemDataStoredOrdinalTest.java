package org.akaza.openclinica.dao.submit;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import org.akaza.openclinica.bean.submit.ItemDataBean;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ItemDataStoredOrdinalTest {
    @Test public void jdbcNullStaysDistinctFromZeroAndOne() throws Exception {
        ItemDataDAO dao = new ItemDataDAO(null);
        dao.setFormatDates(false);
        dao.setTypesExpected();
        ResultSet rows = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(rows.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(10);
        when(metadata.getColumnName(anyInt())).thenReturn("unused");
        when(metadata.getColumnName(10)).thenReturn("ordinal");
        when(rows.getInt(10)).thenReturn(0);
        when(rows.wasNull()).thenReturn(true);
        HashMap<String,Object> fields = dao.processCurrentRow(rows);
        assertTrue(fields.containsKey("ordinal"));
        assertNull(fields.get("ordinal"));
        fields.put("item_data_id", 37);
        fields.put("event_crf_id", 12);
        fields.put("item_id", 6);
        fields.put("status_id", 1);
        fields.put("deleted", false);
        fields.put("owner_id", 1);
        fields.put("update_id", 1);
        ItemDataBean value = dao.getEntityFromHashMap(fields);
        assertNull(value.getStoredOrdinal());
        when(rows.wasNull()).thenReturn(false);
        assertEquals(Integer.valueOf(0), dao.processCurrentRow(rows).get("ordinal"));
        assertEquals(Integer.valueOf(1), new ItemDataBean().getStoredOrdinal());
    }
}
