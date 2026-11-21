/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * copyright (C) 2003 - 2011 Akaza Research
 * copyright (C) 2003 - 2019 OpenClinica
 * copyright (C) 2020 - 2024 LibreClinica
 */
package org.akaza.openclinica.dao.core;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import org.apache.commons.digester.xmlrules.*;

/**
 * <P>
 * Creates an Apache Commons Digester, which parses SQL queries in XML and then
 * stores them in a hashmap, to be accessed later. Idea is to create one XML
 * file per Data Access Object, so that SQL syntax can be abstracted out of the
 * Java JDBC code.
 * </P>
 *
 * @author thickerson
 *
 * TODO
 */
public class DAODigester {

    private final HashMap<String, String> queries = new HashMap<>();
    private final HashMap<String, String[]> projections = new HashMap<>();
    private static final Pattern PROJECTION = Pattern.compile("@native\\(([a-z_]+)(?: ([a-z_]+))?\\)");
    private InputStream fis;

    public void run() throws IOException, SAXException {
        Digester digester = new Digester();
        digester.push(this);
        digester.addCallMethod("queries/projection", "setProjection", 2);
        digester.addCallParam("queries/projection/name", 0);
        digester.addCallParam("queries/projection/columns", 1);
        // set up a simple format for grabbing queries through XML
        /*
         * <queries> <query> <name>userDaoInsert</name> <sql>INSERT INTO USER
         * (USER_ID, USER_NAME, USER_PASS) VALUES (USER_ID_SEQ.NEXTVAL,?,?);</sql>
         * </query> </queries>
         *
         */
        digester.addCallMethod("queries/query", "setQuery", 2);
        digester.addCallParam("queries/query/name", 0);
        digester.addCallParam("queries/query/sql", 1);
        digester.parse(fis);
    }

    /** Named columns of a native positional DAO, in its declared order. Shared EDC tables can have additional columns. */
    public void setProjection(String name, String columns) {
        String[] fields = columns.trim().split("[,\\s]+");
        for (String field : fields) {
            if (!field.matches("[a-z_][a-z_0-9]*")) throw new IllegalArgumentException("Invalid projection column: " + field);
        }
        if (projections.put(name, fields) != null) throw new IllegalArgumentException("Duplicate projection: " + name);
    }

    public void setQuery(String name, String query) {
        Matcher matcher = PROJECTION.matcher(query);
        StringBuffer sql = new StringBuffer();
        while (matcher.find()) {
            String[] fields = projections.get(matcher.group(1));
            if (fields == null) throw new IllegalArgumentException("Unknown native projection: " + matcher.group(1));
            String qualifier = matcher.group(2) == null ? "" : matcher.group(2) + ".";
            matcher.appendReplacement(sql, qualifier + String.join(", " + qualifier, fields));
        }
        matcher.appendTail(sql);
        queries.put(name, sql.toString());
    }

    public String getQuery(String name) {
        return (String) queries.get(name);
    }

    public void setInputStream(InputStream fis) {
        this.fis = fis;
    }

}
