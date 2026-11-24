package org.akaza.openclinica.dao.core;

import junit.framework.TestCase;
import java.io.InputStream;
import java.util.Locale;

public class NativeQueryProjectionTest extends TestCase {
    private DAODigester load(String filename) throws Exception {
        DAODigester queries = new DAODigester();
        try (InputStream source = getClass().getResourceAsStream("/properties/" + filename)) {
            assertNotNull(source);
            queries.setInputStream(source);
            queries.run();
        }
        return queries;
    }

    private void assertProjection(String query, String first, String last, int width) {
        String sql = query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String columns = sql.substring(sql.indexOf(first), sql.indexOf(" from ")).trim();
        String[] selected = columns.split(",\\s*");
        assertEquals(width, selected.length);
        assertEquals(first, selected[0]);
        assertEquals(last, selected[width - 1]);
        assertFalse(columns.contains("*"));
    }

    public void testAccountAndRoleQueriesSelectTheirNativeColumnsInOrder() throws Exception {
        DAODigester accounts = load("useraccount_dao.xml");
        assertProjection(accounts.getQuery("findByUserName"), "user_id", "authsecret", 29);
        assertProjection(accounts.getQuery("findAllByStudyId"), "sur.role_name", "sur.user_name", 8);
    }

    public void testStudyAndSiteQueriesKeepTheSameProjectionAcrossJoins() throws Exception {
        DAODigester studies = load("study_dao.xml");
        assertProjection(studies.getQuery("findByPK"), "study_id", "contact_email", 58);
        assertProjection(studies.getQuery("findSiteByUniqueIdentifier"), "site.study_id", "site.contact_email", 58);
    }

    public void testClinicalImportReadsItsDeclaredColumnsWhenAnApiExtendsTheNativeTables() throws Exception {
        assertProjection(load("study_subject_dao.xml").getQuery("findByOidAndStudy"), "ss.study_subject_id", "ss.time_zone", 13);
        assertProjection(load("studyeventdefinition_dao.xml").getQuery("findByOidAndStudy"), "sed.study_event_definition_id", "sed.oc_oid", 14);
        assertProjection(load("study_event_dao.xml").getQuery("findByStudySubjectIdAndDefinitionIdAndOrdinal"), "study_event_id", "end_time_flag", 15);
        assertProjection(load("crf_dao.xml").getQuery("findByOID"), "crf_id", "source_study_id", 10);
        assertProjection(load("crfversion_dao.xml").getQuery("findByOID"), "crf_version_id", "xform_name", 13);
        assertProjection(load("eventcrf_dao.xml").getQuery("findByPK"), "event_crf_id", "sdv_update_id", 23);
    }
}
