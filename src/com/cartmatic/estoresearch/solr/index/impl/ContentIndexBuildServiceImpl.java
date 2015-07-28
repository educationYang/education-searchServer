package com.cartmatic.estoresearch.solr.index.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.cartmatic.estore.Constants;
import com.cartmatic.estore.common.model.catalog.Category;
import com.cartmatic.estore.common.model.content.Content;
import com.cartmatic.estore.common.util.DateUtil;
import com.cartmatic.estore.content.service.ContentManager;
import com.cartmatic.estore.core.exception.ApplicationException;
import com.cartmatic.estore.textsearch.SearchConstants;

public class ContentIndexBuildServiceImpl extends AbstractIndexBuildServiceImpl
{
    private SolrServer solrServer;
    private ContentManager contentManager;
    private static final Log logger = LogFactory.getLog(ContentIndexBuildServiceImpl.class);
    
    public void init()
    {
        logger.info("ContentIndexBuildService init.");
        solrServer = solrService.getSolrServer(SearchConstants.CORE_NAME_CONTENT);
    }
    
    @Override
    public void buildIndex(SearchConstants.UPDATE_TYPE type, List<Integer> ids, String hql)
    {
        if (SearchConstants.UPDATE_TYPE.REBUILD_ALL.equals(type))
        {
            rebuild();
            return;
        }
        if (ids != null)  //根据输入的主键
        {
            for (Integer pid : ids)
            {
                //processIndex(contentManager.getById(pid), type);
                try
                {
                    if (SearchConstants.UPDATE_TYPE.UPDATE.equals(type))
                    {
                        solrServer.add(getDoc(contentManager.getById(pid)));
                    }
                    else if (SearchConstants.UPDATE_TYPE.DEL.equals(type))
                    {
                        solrServer.deleteById(pid.toString());
                    }
                }
                catch (Exception e)
                {
                    logger.error("Adding doc to solr is Failed.", e);
                }
            }
            solrService.flushChanges(solrServer, false);            
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Index was builded and going to be submit.");
        }
        solrService.flushChanges(solrServer, true);
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
    
    //重新插入所有的索引
    private void addAllDoc(SolrServer server)
    {
        //状态是active
        String hql = "select p from Content p where p.status=1";
        List<Content> rs = null;
        int page = 0;
        do
        {
            rs = appEventDao.fetchEntitysToProcess(hql, 50, page);
            page++;
            for (int i = 0; i < rs.size(); i++)
            {
                //processIndex(rs.get(i), SearchConstants.UPDATE_TYPE.UPDATE);
                try
                {
                    solrServer.add(getDoc(rs.get(i)));
                }
                catch (Exception e)
                {
                    logger.error("Adding doc to solr is Failed.", e);
                    e.printStackTrace();
                }                
            }
            try
            {
                solrService.flushChanges(solrServer, false);
            }
            catch(ApplicationException e)
            {
                logger.warn("Can not flushCaches.");
            }
        }
        while (rs != null && rs.size() == 50);
    }
    
//    private void processIndex(Content vo, SearchConstants.UPDATE_TYPE indexType)
//    {
//        try
//        {
//            if (SearchConstants.UPDATE_TYPE.UPDATE.equals(indexType))
//            {
//                solrServer.add(getDoc(vo));
//            }
//            else if (SearchConstants.UPDATE_TYPE.DEL.equals(indexType))
//            {
//                solrServer.deleteById(vo.getId().toString());
//            }
//        }
//        catch (Exception e)
//        {
//            logger.error("Adding doc to solr is Failed.", e);
//        }
//    }
    
    /**
     * 组合Document
     * @param vo
     * @return
     */
    private SolrInputDocument getDoc(Content vo)
    {
        SolrInputDocument doc = new SolrInputDocument();
        // doc.setDocumentBoost(documentBoost);
        //id
        doc.addField("id", vo.getId());
        //storeId
        doc.addField("storeId", vo.getStoreId());
        //contentTitle
        doc.addField("contentTitle", vo.getContentTitle(), 1.0f);
        //keyword
        doc.addField("keyword", vo.getMetaKeyword());
        //contentCode
        doc.addField("contentCode", vo.getContentCode());
        //parentCategoryIds 应该包括所有父目录
        //TODO 应该包括所有父目录
        doc.addField("parentCategoryIds", vo.getCategoryId());
        List<Category> cats = vo.getCategory().getAllParentCategorys();
        for (Category pc : cats)
        {
            doc.addField("parentCategoryIds", pc.getCategoryId());            
        }
        //displayable
        if (Constants.STATUS_ACTIVE.equals(vo.getStatus()))
            doc.addField("displayable", Boolean.TRUE);
        else
            doc.addField("displayable", Boolean.FALSE);
        //expiredTime
        if (vo.getExpiredTime() == null) //如果没有设置过期时间,就指定一个2099年,假设不会过期
            doc.addField("expiredTime", DateUtil.getNeverEpiredDate());
        else
            doc.addField("expiredTime", vo.getExpiredTime());
        //内容
        doc.addField("contentBody",vo.getContentBody());        
        return doc;
    }    
    
    public void setContentManager(ContentManager avalue)
    {
        contentManager = avalue;
    }
}
