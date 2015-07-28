package com.cartmatic.estoresearch.solr.index;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.cartmatic.estore.core.event.AppEventHandler;
import com.cartmatic.estore.framework.test.BaseTransactionalTestCase;
import com.cartmatic.estore.textsearch.SearchConstants;
import com.cartmatic.estore.webapp.event.IndexNotifyEvent;

public class TestProductIndex extends BaseTransactionalTestCase 
{
    @Autowired
    private AppEventHandler indexNotifyEventHandler = null;
    
    @Test
    public void testHandler() throws Exception {
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(3423);
        IndexNotifyEvent event = new IndexNotifyEvent(SearchConstants.CORE_NAME_PRODUCT,
                        SearchConstants.UPDATE_TYPE.DEL);
        event.addId(232);
        indexNotifyEventHandler.handleApplicationEvent(event);
          
        //this.setComplete(); 
    }

    
}
