package com.cartmatic.estoresearch.solr.index.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.cartmatic.estore.common.model.catalog.Product;
import com.cartmatic.estore.common.service.SolrService;
import com.cartmatic.estore.core.exception.ApplicationException;
import com.cartmatic.estore.system.dao.AppEventDao;
import com.cartmatic.estore.textsearch.SearchConstants;
import com.cartmatic.estoresearch.solr.index.AbcIndexBuildService;

public class AbcIndexBuildServiceImpl implements AbcIndexBuildService
{
    private SolrService solrService;
    private SolrServer solrServer;
    private AppEventDao appEventDao;
    private static final Log logger = LogFactory.getLog(ProductIndexBuidServiceImpl.class);

    public void init()
    {
        logger.info("ProductIndexBuidService init.");
        solrServer = solrService.getSolrServer(SearchConstants.CORE_NAME_PRODUCT);
    }

    public void buildIndex()
    {
        //get 最新update过的product getAllNewEntiy(Date date)
    }
    
    /**
     * buildIndex这个方法是被handler定时调用的。
     * 根据update_type和hql来对索引进行操作
     */
    public void buildIndex(SearchConstants.UPDATE_TYPE type, String hql)
    {
        if (SearchConstants.UPDATE_TYPE.REBUILD_ALL.equals(type))
        {
            rebuild();
            return;
        }
        List<Product> rs = null;
        int page = 0;
        do
        {
            rs = appEventDao.fetchEntitysToProcess(hql, 50, page);
            page++;
            for (int i = 0; i < rs.size(); i++)
            {
                processIndex(rs.get(i), type);
                
            }
            solrService.flushChanges(solrServer, false);
        }
        while (rs != null && rs.size() == 50);
        if (logger.isDebugEnabled())
        {
            logger.debug("Index was builded and going to be submit.");
        }
        solrService.flushChanges(solrServer, true);
    }
    
    private void processIndex(Product vo, SearchConstants.UPDATE_TYPE indexType)
    {
        try
        {
            if (SearchConstants.UPDATE_TYPE.UPDATE.equals(indexType))
            {
                solrServer.add(getDoc(vo));
            }
            else if (SearchConstants.UPDATE_TYPE.DEL.equals(indexType))
            {
                solrServer.deleteById(vo.getId().toString());
            }
        }
        catch (Exception e)
        {
            logger.error("Adding doc to solr is Failed.", e);
        }
    }

    private SolrInputDocument getDoc(Product vo)
    {
        SolrInputDocument doc = new SolrInputDocument();
        // doc.setDocumentBoost(documentBoost);
        doc.addField("id", vo.getProductId());
        doc.addField("productName", vo.getProductName(), 1.0f);
        if (vo.getBrand() != null)
            doc.addField("brandCode", vo.getBrand().getBrandCode());
        doc.addField("title", vo.getTitle());
        if (vo.getDefaultProductSku() != null)
            doc.addField("sku", vo.getDefaultProductSku().getProductSkuCode());
        doc.addField("updateTime", vo.getUpdateTime());
        return doc;
    }    

    private void rebuild()
    {
        try
        {
            removeAllIndexes(solrServer);
            addAllDoc(solrServer);
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }
    
    private void addAllDoc(SolrServer server)
    {
        // getAllActiveEntityHql() 状态是active，而且父目录也是active
    }

    private void removeAllIndexes(SolrServer server) throws ApplicationException
    {
        if (logger.isDebugEnabled())
            logger.debug("Removing all indexes from server "+server.toString());
        try
        {
            server.deleteByQuery("*:*");
        }
        catch (SolrServerException e)
        {
            throw new ApplicationException("remove all indexes", e);
        }
        catch (IOException e)
        {
            throw new ApplicationException("remove all indexes", e);
        }
        solrService.flushChanges(server, true);
    }

    public void setSolrService(SolrService avalue)
    {
        solrService = avalue;
    }

    public void setAppEventDao(AppEventDao avalue)
    {
        appEventDao = avalue;
    }
}
