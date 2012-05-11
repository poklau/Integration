package com.versionone.git.storage;

import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class DbStorage implements IDbStorage {
    private final String LAST_COMMIT_HASH = "LastCommitHash";
    private final Session session;

    private Session getSession() {
        return session;
    }

    public DbStorage() {
        SessionFactory factory = new Configuration().configure().buildSessionFactory();
        session = factory.openSession();
    }

    public List<PersistentChange> getPersistedChanges() {
        List changes = getSession().createCriteria(PersistentChange.class).list();
        return (List<PersistentChange>)changes;
    }

    public void persistChange(PersistentChange change) {
        Transaction tr = getSession().beginTransaction();
        getSession().save(change);
        tr.commit();
        getSession().flush();
    }

    public boolean isChangePersisted(PersistentChange change) {
        Criteria criteria = getSession()
                .createCriteria(PersistentChange.class)
                .add(Restrictions.eq("hash", change.getHash()))
                .add(Restrictions.eq("repositoryId", change.getRepositoryId()));
        Integer count = (Integer) criteria.setProjection(Projections.rowCount()).uniqueResult();
        return count > 0;
    }

    public void persistLastCommit(String commitHash, String repositoryId) {
        LastProcessedItem lastHash = new LastProcessedItem();
        //lastHash.setId(LAST_COMMIT_HASH + "||" + repositoryId);
        lastHash.setValue(commitHash);
        lastHash.setRepositoryId(repositoryId);

        Transaction tr = getSession().beginTransaction();
        getSession().saveOrUpdate(lastHash);
        tr.commit();
        getSession().flush();
    }

    public String getLastCommit(String repositoryId){
        Criteria criteria = getSession().
                                    createCriteria(LastProcessedItem.class).
                                    //add(Restrictions.eq("id", LAST_COMMIT_HASH + "||" + repositoryId)).
                                    add(Restrictions.eq("repositoryId", repositoryId));
        LastProcessedItem result = (LastProcessedItem)criteria.uniqueResult();
        getSession().evict(result);
        return result == null ? null : result.getValue();
    }
}