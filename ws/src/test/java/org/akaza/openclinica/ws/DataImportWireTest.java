package org.akaza.openclinica.ws;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.Test;
import org.springframework.ws.server.endpoint.annotation.XPathParam;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import static org.junit.Assert.*;

public class DataImportWireTest {
    private Element select(String payload) throws Exception {
        String xml = "<v1:importRequest xmlns:v1='http://openclinica.org/ws/data/v1'>" + payload + "</v1:importRequest>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        String xpath = ((XPathParam) DataEndpoint.class.getMethod("importData", Element.class)
                .getParameterAnnotations()[0][0]).value();
        return (Element) XPathFactory.newInstance().newXPath().evaluate(xpath,
                factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))), XPathConstants.NODE);
    }

    @Test public void readsOneOriginalOdmElementInTheRequest() throws Exception {
        String canonical = "<o:ODM xmlns:o='http://www.cdisc.org/ns/odm/v1.3' FileOID='SOURCE'>"
                + "<o:ClinicalData StudyOID='S'/></o:ODM>";
        assertEquals("SOURCE", select(canonical).getAttribute("FileOID"));
        assertEquals("S", ((Element)select(canonical).getFirstChild()).getAttribute("StudyOID"));
        assertNotNull(select("<v1:odm><ODM/></v1:odm>"));
        assertNull(select("<v1:odm><![CDATA[" + canonical + "]]></v1:odm>"));
        assertNull(select("<ODM xmlns='urn:other'/>"));
        assertNull(select(canonical + canonical));
    }
}
