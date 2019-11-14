package org.openelisglobal.reportconfiguration.daoimpl;

import java.util.List;

import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reportconfiguration.dao.ReportDAO;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReportDAOImpl extends BaseDAOImpl<Report, String> implements ReportDAO {

    public ReportDAOImpl() {
        super(Report.class);
    }

    @Override
    public int getMaxSortOrder(String category) {
        try {
            String sql = "SELECT max(r.sortOrder) from Report r where r.category = :category";
            org.hibernate.Query query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setString("category", category);
            List list = query.list();
            if (list.size() > 0) {
                return (Integer) list.get(0);
            }
            return -1;

        } catch (Exception e) {
            LogEvent.logError("ReportDAOImpl", "getMaxSortOrder()", e.toString());
            throw new LIMSRuntimeException("Error in ReportDAOImpl getMaxSortOrder()", e);
        }
    }
}
