package com.cartmatic.estoresearch.solr.index.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.cartmatic.estore.Constants;
import com.cartmatic.estore.catalog.CatalogConstants;
import com.cartmatic.estore.catalog.service.ProductStatManager;
import com.cartmatic.estore.common.model.attribute.ProductAttrValue;
import com.cartmatic.estore.common.model.catalog.Accessory;
import com.cartmatic.estore.common.model.catalog.Catalog;
import com.cartmatic.estore.common.model.catalog.Category;
import com.cartmatic.estore.common.model.catalog.Product;
import com.cartmatic.estore.common.model.catalog.ProductCategory;
import com.cartmatic.estore.common.model.catalog.ProductSku;
import com.cartmatic.estore.common.model.catalog.ProductSkuOptionValue;
import com.cartmatic.estore.common.model.catalog.ProductStat;
import com.cartmatic.estore.common.model.catalog.SkuOption;
import com.cartmatic.estore.common.model.catalog.SkuOptionValue;
import com.cartmatic.estore.common.model.inventory.Inventory;
import com.cartmatic.estore.common.model.system.Store;
import com.cartmatic.estore.common.service.ProductService;
import com.cartmatic.estore.common.util.DateUtil;
import com.cartmatic.estore.core.exception.ApplicationException;
import com.cartmatic.estore.textsearch.SearchConstants;

public class ProductIndexBuidServiceImpl extends AbstractIndexBuildServiceImpl
{
    
    private SolrServer solrServer;
    
    private ProductService productService;
    private ProductStatManager productStatManager;
    
    
    public void setProductStatManager(ProductStatManager productStatManager) {
		this.productStatManager = productStatManager;
	}



	private static final Log logger = LogFactory.getLog(ProductIndexBuidServiceImpl.class);

    public void init()
    {
        logger.info("ProductIndexBuidService init.");
        solrServer = solrService.getSolrServer(SearchConstants.CORE_NAME_PRODUCT);
    }

    /**
     * buildIndex这个方法是被handler定时调用的。
     * 根据update_type和hql来对索引进行操作
     */
    @Override
    public void buildIndex(SearchConstants.UPDATE_TYPE type, List<Integer> ids, String hql)
    {
        if (SearchConstants.UPDATE_TYPE.REBUILD_ALL.equals(type))
        {
            rebuild();
            return;
        }
        
        if (hql != null)   //根据hql查询
        {
            List<Product> rs = null;
            int page = 0;
            do
            {
                rs = appEventDao.fetchEntitysToProcess(hql, 50, page);
                page++;
                for (int i = 0; i < rs.size(); i++)
                {
                    processIndex(rs.get(i).getId(), type);
                }
                solrService.flushChanges(solrServer, false);
            }
            while (rs != null && rs.size() == 50);
        }        
        if (ids != null)  //根据输入的主键
        {
            for (Integer pid : ids)
            {
                processIndex(pid, type);
            }
            solrService.flushChanges(solrServer, false);            
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Index was builded and going to be submit.");
        }
        solrService.flushChanges(solrServer, true);
    }
    
    private void processIndex(Integer id, SearchConstants.UPDATE_TYPE indexType)
    {
        try
        {
            if (SearchConstants.UPDATE_TYPE.UPDATE.equals(indexType))
            {
                solrServer.add(getDoc(productService.getProduct(id)));
            }
            else if (SearchConstants.UPDATE_TYPE.DEL.equals(indexType))
            {
                solrServer.deleteById(id.toString());
            }
        }
        catch (Exception e)
        {
            logger.error("Adding doc to solr is Failed.", e);
        }
    }

    /**
     * 组合Document
     * @param vo
     * @return
     */
    private SolrInputDocument getDoc(Product vo)
    {
        SolrInputDocument doc = new SolrInputDocument();
        // doc.setDocumentBoost(documentBoost);
        //id
        doc.addField("id", vo.getProductId());
        //productName
        doc.addField("productName", vo.getProductName(), 1.0f);
        doc.addField("productName_s", vo.getProductName());
        if (vo.getBrand() != null)
            doc.addField("brandId", vo.getBrand().getBrandId());
        //keyword
        doc.addField("keyword", vo.getMetaKeyword());
        //productSkuCode
        doc.addField("productSkuCode", vo.getDefaultProductSku().getProductSkuCode());
        
        
        
        //productSkuCode
        Set<ProductSku> productSkus=vo.getProductSkus();
        int i_inventory = 0;
        for (ProductSku productSku : productSkus) {
			if(productSku.getStatus().intValue()==Constants.STATUS_ACTIVE){
		        doc.addField("productSkuCode", productSku.getProductSkuCode());
		        
		        //库存数据
		        Short availabilityRule=vo.getAvailabilityRule();
		        if (availabilityRule!=CatalogConstants.PRODUCT_AVAILABILITY_ALWAYS_SELL.intValue()&&availabilityRule!=CatalogConstants.PRODUCT_AVAILABILITY_NOT_IN_STOCK_SELL.intValue()) {
		        	Set<Inventory>inventories=productSku.getInventorys();
		        	if(inventories!=null&&inventories.size()>0){
		        		for (Inventory inventory : inventories)
						{
				        	if(inventory!=null&&inventory.getAvailableQuantity()>0){
					        	i_inventory += inventory.getAvailableQuantity();
				        	}
						}
		        	}
		        }
		        
				//sku属性
				if(vo.getProductKind().intValue()==CatalogConstants.PRODUCT_KIND_VARIATION.intValue()){
					Map<SkuOption, List<SkuOptionValue>> productSkuOptionAndValue = new HashMap<SkuOption, List<SkuOptionValue>>();
			        Set<ProductSkuOptionValue>productSkuOptionValues=productSku.getProductSkuOptionValues();
			        if(productSkuOptionValues!=null){
			        	for (ProductSkuOptionValue productSkuOptionValue : productSkuOptionValues) {
							SkuOptionValue skuOptionValue = productSkuOptionValue.getSkuOptionValue();
							SkuOption skuOption = skuOptionValue.getSkuOption();
							List<SkuOptionValue> skuOptionValues = productSkuOptionAndValue.get(skuOption);
							if (skuOptionValues == null) {
								skuOptionValues = new ArrayList<SkuOptionValue>();
								productSkuOptionAndValue.put(skuOption, skuOptionValues);
							}
							skuOptionValues.add(skuOptionValue);
						}
			        }
			        Set<SkuOption> skuOptions=productSkuOptionAndValue.keySet();
			        if(skuOptions!=null){
			        	for (SkuOption skuOption : skuOptions)
						{
			        		List<SkuOptionValue> skuOptionValues=productSkuOptionAndValue.get(skuOption);
			        		for (SkuOptionValue skuOptionValue : skuOptionValues)
							{
			        			 doc.addField("sku_"+skuOption.getSkuOptionCode()+"_s", skuOptionValue.getSkuOptionValue().trim());
							}
						}
			        }
				}
			}
		}
        
        
       //TODO 临时把库存放入boost中
        if (i_inventory > 0)
        	doc.addField("inventory", i_inventory);
        
        //parentCategoryIds 应该包括所有父目录
        //TODO 应该包括所有父目录
        Set<ProductCategory> cats = vo.getProductCategorys();
        List<Integer> catalogIdList=new ArrayList<Integer>();
        List<Integer> storeIdList=new ArrayList<Integer>();
        for (ProductCategory pc : cats)
        {
            doc.addField("parentCategoryIds", pc.getCategoryId());
            List<Category> parentCats = pc.getCategory().getAllParentCategorys();
            if(!catalogIdList.contains(pc.getCategory().getCatalogId())){
            	catalogIdList.add(pc.getCategory().getCatalogId());
            	
            	Catalog catalog=pc.getCategory().getCatalog();
            	Set<Store> stores=catalog.getStores();
            	if(stores!=null){
            		for (Store store : stores) {
            			if(!storeIdList.contains(store.getStoreId())){
                			storeIdList.add(store.getStoreId());
            			}
					}
            	}
            }
            for (Category parentCat: parentCats)
            {
                doc.addField("parentCategoryIds", parentCat.getCategoryId());
                if(!catalogIdList.contains(pc.getCategory().getCatalogId())){
                	catalogIdList.add(pc.getCategory().getCatalogId());
                	
                	Catalog catalog=pc.getCategory().getCatalog();
                	Set<Store> stores=catalog.getStores();
                	if(stores!=null){
                		for (Store store : stores) {
                			if(!storeIdList.contains(store.getStoreId())){
                				storeIdList.add(store.getStoreId());
                			}
    					}
                	}
                }
            }
        }
        //指定所在的catalog
        for (Integer catalogId : catalogIdList) {
            doc.addField("catalogIds", catalogId);
		}
        
        for (Integer storeId : storeIdList) {
            //salesCount 与商店组合
        	ProductStat productStat=productStatManager.getProductStat(storeId, vo.getProductId());
        	doc.addField(storeId+"_salesCount_i", productStat.getBuyCount());
        	doc.addField(storeId+"_favoriteCount_i", productStat.getFavoriteCount());
		}
        
        //price 如果有salesPrice,以salesPrice优先
        if (vo.getDefaultProductSku().getSalePrice() == null)
            doc.addField("price", vo.getDefaultProductSku().getPrice().toString());
        else
            doc.addField("price", vo.getDefaultProductSku().getSalePrice().toString());
        //displayable
        if (Constants.STATUS_ACTIVE.equals(vo.getStatus()))
            doc.addField("displayable", Boolean.TRUE);
        else
            doc.addField("displayable", Boolean.FALSE);
        //createTime
        doc.addField("createTime", vo.getPublishTime());
        //updateTime
        doc.addField("updateTime", vo.getUpdateTime());
        //产品图片（sku.image）
        doc.addField("image",vo.getDefaultProductSku().getImage());
        //产品URL（非访问产品Url，而是产品本身的属性）
        doc.addField("url",vo.getUrl());
        
        //TODO 自定义属性
        Set<ProductAttrValue> productAttrValues=vo.getProductAttrValues();
        if(productAttrValues!=null){
        	for (ProductAttrValue productAttrValue : productAttrValues) {
        		String code=productAttrValue.getAttribute().getAttributeCode();
        		
        		if (productAttrValue.getAttribute().getAttributeDataType() == com.cartmatic.estore.attribute.Constants.DATETYPE_CHECKBOX.intValue()){
        			String v = productAttrValue.getAttributeValue().toString();
        			for (String value: v.split(","))
        			{
        				doc.addField(code+"_s",value.trim());
        			}
        		}
        		if (productAttrValue.getAttribute().getAttributeDataType() == com.cartmatic.estore.attribute.Constants.DATETYPE_INT.intValue()){
        			doc.addField(code+"_i",productAttrValue.getAttributeValue().toString().trim());
        		}
        		else if (productAttrValue.getAttribute().getAttributeDataType() == com.cartmatic.estore.attribute.Constants.DATETYPE_FLOAT.intValue()){
        			doc.addField(code+"_f",productAttrValue.getAttributeValue().toString().trim());
        		}
        		else if (productAttrValue.getAttribute().getAttributeDataType() == com.cartmatic.estore.attribute.Constants.DATETYPE_BOOLEAN.intValue()){
        			doc.addField(code+"_b",productAttrValue.getAttributeValue().toString().trim());
        		}
        		else if (productAttrValue.getAttribute().getAttributeDataType() == com.cartmatic.estore.attribute.Constants.DATETYPE_DATE.intValue()){
        			doc.addField(code+"_dt",productAttrValue.getAttributeValue().toString().trim());
        		}
        		else if (productAttrValue.getAttribute().getAttributeDataType() == com.cartmatic.estore.attribute.Constants.DATETYPE_LONGTEXT.intValue()){
        			doc.addField(code+"_t",productAttrValue.getAttributeValue().toString().trim());
        		}else {
        			doc.addField(code+"_s",productAttrValue.getAttributeValue().toString().trim());
        		}
    		}
        }
        //附件组
        /*Set<Accessory> productAccessories=vo.getAccessories();
        for (Accessory accessory : productAccessories) {
        	String code=accessory.getAccessoryGroup().getGroupCode();
        	doc.addField(code+"_s",accessory.getAccessoryName().toString().trim());
		}*/
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
    
    //重新插入所有产品的索引
    private void addAllDoc(SolrServer server)
    {
        //状态是active
        String hql = "select p from Product p where p.status=1";
        List<Product> rs = null;
        int page = 0;
        do
        {
            rs = appEventDao.fetchEntitysToProcess(hql, 50, page);
            page++;
            for (int i = 0; i < rs.size(); i++)
            {
                processIndex(rs.get(i).getId(), SearchConstants.UPDATE_TYPE.UPDATE);
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

    

    //setter
    
    public void setProductService(ProductService avalue)
    {
        productService = avalue;
    }
}
