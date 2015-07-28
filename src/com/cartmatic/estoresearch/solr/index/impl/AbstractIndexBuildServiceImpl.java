package com.cartmatic.estoresearch.solr.index.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

import com.cartmatic.estore.common.service.SolrService;
import com.cartmatic.estore.core.exception.ApplicationException;
import com.cartmatic.estore.system.dao.AppEventDao;
import com.cartmatic.estore.textsearch.SearchConstants.UPDATE_TYPE;
import com.cartmatic.estoresearch.solr.index.IndexBuildService;

public abstract class AbstractIndexBuildServiceImpl  implements IndexBuildService
{
    protected SolrService solrService;
    protected AppEventDao appEventDao;
    private static final Log logger = LogFactory.getLog(AbstractIndexBuildServiceImpl.class);
    
    /**
     * 删除所有索引
     * @param server
     * @throws ApplicationException
     */
    protected void removeAllIndexes(SolrServer server) throws ApplicationException
    {
        if (logger.isDebugEnabled())
            logger.debug("Removing all indexes from server "+server.toString());
        try
        {
            server.deleteByQuery("*:*");
        }
        catch (SolrServerException e)
        {
            logger.error(e);
            throw new ApplicationException("remove all indexes", e);
        }
        catch (IOException e)
        {
            logger.error(e);
            throw new ApplicationException("remove all indexes", e);
        }
        solrService.flushChanges(server, true);
    }
    
    public abstract void buildIndex(UPDATE_TYPE type, List<Integer> ids, String hql);
    
    //setter
    public void setSolrService(SolrService avalue)
    {
        solrService = avalue;
    }
    public void setAppEventDao(AppEventDao avalue)
    {
        appEventDao = avalue;
    }
}
