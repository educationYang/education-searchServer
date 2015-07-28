package com.cartmatic.estoresearch.webapp.event;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.cartmatic.estore.core.event.AppEventHandler;
import com.cartmatic.estore.core.event.ApplicationEvent;
import com.cartmatic.estore.core.util.ContextUtil;
import com.cartmatic.estore.system.dao.AppEventDao;
import com.cartmatic.estore.textsearch.SearchConstants;
import com.cartmatic.estore.webapp.event.IndexNotifyEvent;
import com.cartmatic.estoresearch.solr.index.IndexBuildService;

public class IndexNotifyEventHandler implements AppEventHandler
{
    private static final Log logger = LogFactory.getLog(IndexNotifyEventHandler.class);
    
    public void handleApplicationEvent(ApplicationEvent event) 
    {
        if (event instanceof IndexNotifyEvent)
        {
            IndexNotifyEvent inEvent = (IndexNotifyEvent) event;
            if (logger.isDebugEnabled())
                logger.debug("Processing index event: " + inEvent.getCore());
            if (SearchConstants.CORE_NAME_PRODUCT.equals(inEvent.getCore()))
            {
                IndexBuildService productIndexBuilder = (IndexBuildService) ContextUtil.getSpringBeanById("productIndexBuildService");
                productIndexBuilder.buildIndex(inEvent.getUpdateType(), inEvent.getIds(), inEvent.getHql());
            }
            else if (SearchConstants.CORE_NAME_SALESORDER.equals(inEvent.getCore()))
            {
                IndexBuildService contentIndexBuilder = (IndexBuildService) ContextUtil.getSpringBeanById("salesOrderIndexBuildService");
                contentIndexBuilder.buildIndex(inEvent.getUpdateType(), inEvent.getIds(), inEvent.getHql());
            }
            else if (SearchConstants.CORE_NAME_CONTENT.equals(inEvent.getCore()))
            {
                IndexBuildService contentIndexBuilder = (IndexBuildService) ContextUtil.getSpringBeanById("contentIndexBuildService");
                contentIndexBuilder.buildIndex(inEvent.getUpdateType(), inEvent.getIds(), inEvent.getHql());
            }
        }
    }
    
    
    
}
