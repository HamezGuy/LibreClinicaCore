/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * copyright (C) 2003 - 2011 Akaza Research
 * copyright (C) 2003 - 2019 OpenClinica
 * copyright (C) 2020 - 2024 LibreClinica
 */
package org.akaza.openclinica.dao.hibernate;

import java.sql.Timestamp;
import java.util.Objects;

import org.akaza.openclinica.domain.OpenClinicaVersionBean;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author pgawade
 */
public class OpenClinicaVersionDAO extends AbstractDomainDao<OpenClinicaVersionBean> {

    @Override
    public Class<OpenClinicaVersionBean> domainClass() {
        return OpenClinicaVersionBean.class;
    }

    @Transactional
    public OpenClinicaVersionBean findDefault() {
        String query = "from " + getDomainClassName() + " ocVersion order by ocVersion.id desc";
        Query<OpenClinicaVersionBean> q = getCurrentSession().createQuery(query, OpenClinicaVersionBean.class);
        return q.setMaxResults(1).uniqueResult();
    }

    @Transactional
    public void saveOCVersionToDB(String version) {
        // Serialize startup writers, including the first writer when there is no row to lock.
        getCurrentSession().createNativeQuery("LOCK TABLE openclinica_version IN EXCLUSIVE MODE").executeUpdate();
        OpenClinicaVersionBean current = findDefault();
        if (current != null && Objects.equals(current.getName(), version)) {
            return;
        }

        OpenClinicaVersionBean next = new OpenClinicaVersionBean();
        next.setName(version);
        next.setUpdate_timestamp(new Timestamp(System.currentTimeMillis()));
        saveOrUpdate(next);
    }

}
