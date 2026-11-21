/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * copyright (C) 2003 - 2011 Akaza Research
 * copyright (C) 2003 - 2019 OpenClinica
 * copyright (C) 2020 - 2024 LibreClinica
 */
package org.akaza.openclinica.service.rule;

import java.util.Locale;
import java.util.ResourceBundle;
import junit.framework.TestCase;
import org.akaza.openclinica.bean.managestudy.StudyBean;
import org.akaza.openclinica.domain.rule.RuleBean;
import org.akaza.openclinica.domain.rule.RulesPostImportContainer;
import org.akaza.openclinica.domain.rule.expression.ExpressionBean;

public class RulesPostImportContainerServiceTest extends TestCase {
    public void testInvalidRuleOidIsReportedWithoutPersistingTheRule() {
        StudyBean study = new StudyBean();
        study.setId(17);
        RulesPostImportContainerService service = new RulesPostImportContainerService(null, study);
        service.setRespage(ResourceBundle.getBundle("org.akaza.openclinica.i18n.page_messages", Locale.ENGLISH));
        RuleBean rule = new RuleBean();
        rule.setOid("NOT A RULE OID");
        ExpressionBean expression = new ExpressionBean();
        expression.setValue("1 eq 1");
        rule.setExpression(expression);
        RulesPostImportContainer imported = new RulesPostImportContainer();
        imported.addRuleDef(rule);

        service.validateRuleDefs(imported);

        assertEquals(1, imported.getInValidRuleDefs().size());
        assertSame(rule, imported.getInValidRules().get(rule.getOid()).getAuditableBean());
        assertTrue(imported.getValidRuleDefs().isEmpty());
        assertTrue(imported.getDuplicateRuleDefs().isEmpty());
    }
}
