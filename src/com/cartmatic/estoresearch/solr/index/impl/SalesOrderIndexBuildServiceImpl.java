package com.cartmatic.estoresearch.solr.index.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.cartmatic.estore.common.model.order.OrderShipment;
import com.cartmatic.estore.common.model.order.OrderSku;
import com.cartmatic.estore.core.exception.ApplicationException;
import com.cartmatic.estore.order.service.SalesOrderManager;
import com.cartmatic.estore.textsearch.SearchConstants;
import com.cartmatic.estore.textsearch.SearchConstants.UPDATE_TYPE;

/**
 * 对订单进行索引
 * 只针对已经完成发货的订单.以orderShipment为单位
 * @author Administrator
 *
 */
public class SalesOrderIndexBuildServiceImpl  extends AbstractIndexBuildServiceImpl
{

    //private SolrService solrService;
    private SolrServer solrServer;
    private SalesOrderManager salesOrderManager;
    private static final Log logger = LogFactory.getLog(SalesOrderIndexBuildServiceImpl.class);
    
    public void init()
    {
        logger.info("SalesOrderIndexBuildService init.");
        solrServer = solrService.getSolrServer(SearchConstants.CORE_NAME_SALESORDER);
    }
    
    /**
     * ids为ordershipment的主键
     */
    public void buildIndex(UPDATE_TYPE type, List<Integer> ids, String hql)
    {
        if (SearchConstants.UPDATE_TYPE.REBUILD_ALL.equals(type))
        {
            rebuild();
            return;
        }
        // TODO Auto-generated method stub
        if (ids == null || ids.isEmpty())
        {
            return;
        }
        
        for (Integer orderShipmentId : ids)
        {
            OrderShipment obj = salesOrderManager.getOrderShipmentById(orderShipmentId);
            processIndex(obj, type);
        }
        solrService.flushChanges(solrServer, false);
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
    //重新插入所有产品的索引
    private void addAllDoc(SolrServer server)
    {
        //状态是已经发货的
        String hql = "select p from OrderShipment p where p.status=60";
        List<OrderShipment> rs = null;
        int page = 0;
        do
        {
            rs = appEventDao.fetchEntitysToProcess(hql, 50, page);
            page++;
            for (int i = 0; i < rs.size(); i++)
            {
                processIndex(rs.get(i), SearchConstants.UPDATE_TYPE.UPDATE);
            }
            try
            {
                solrService.flushChanges(solrServer, false);
            }
            catch(ApplicationException e)
            {
                logger.warn("Can not flushCaches.");
                //do nothing.
            }
        }
        while (rs != null && rs.size() == 50);
    }
    
    private void processIndex(OrderShipment vo, SearchConstants.UPDATE_TYPE indexType)
    {
        try
        {
            if (SearchConstants.UPDATE_TYPE.UPDATE.equals(indexType))
            {
                try
                {
                    Set<OrderSku> skus = vo.getOrderSkus();
                    for (OrderSku sku : skus)
                    {
                        solrServer.add(getDoc(sku));
                    }
                }
                catch (Exception e)
                {
                    logger.error("Adding doc to solr is Failed.", e);
                }
            }
            else if (SearchConstants.UPDATE_TYPE.DEL.equals(indexType))
            {
                Set<OrderSku> skus = vo.getOrderSkus();
                for (OrderSku sku : skus)
                {
                    solrServer.deleteById(sku.getId().toString());
                }
                
            }
        }
        catch (Exception e)
        {
            logger.error("Adding doc to solr is Failed.", e);
        }
    }
    /**
     * orderNo qty subtotal trackingNo productSkuCode productName supplierId orderShipmentId deliverTime createTime
     * 
     * @param sku
     * @return
     */
    private SolrInputDocument getDoc(OrderSku sku)
    {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", sku.getOrderSkuId());
        doc.addField("orderNo", sku.getOrderShipment().getSalesOrder().getOrderNo());
        doc.addField("qty", sku.getQuantity());
        doc.addField("subtotal", sku.getDiscountPrice().multiply(new BigDecimal(sku.getQuantity())));
        doc.addField("trackingNo", sku.getOrderShipment().getTrackingNo());
        doc.addField("productSkuCode", sku.getProductSkuCode());
        doc.addField("productName", sku.getProductName());
        doc.addField("supplierId", sku.getProductSku().getProduct().getSupplierId());
        doc.addField("orderShipmentId", sku.getOrderShipment().getOrderShipmentId());
        doc.addField("deliverTime", sku.getOrderShipment().getDeliverTime());
        doc.addField("createTime", sku.getOrderShipment().getCreateTime());
        return doc;
    }
    
  //setter
    public void setSalesOrderManager(SalesOrderManager avalue)
    {
        salesOrderManager = avalue;
    }
}
