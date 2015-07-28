package com.cartmatic.estoresearch.solr.index;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.cartmatic.estore.framework.test.BaseTransactionalTestCase;
import com.cartmatic.estore.textsearch.SearchConstants;

public class ProductIndexBuildTest extends BaseTransactionalTestCase
{
    @Autowired
    private AbcIndexBuildService abcIndexBuildService;
    //private IndexBuildService pi;


    
    @Test
    public void testBuildIndex()
    {
        abcIndexBuildService.buildIndex(SearchConstants.UPDATE_TYPE.UPDATE, "select vo from Product vo");
    }
    
    
    /*public void setProductBuildIndexService(IndexBuildService avalue)
    {
        pi = avalue;
    }*/
}
