package com.cartmatic.estoresearch.solr.index;

import com.cartmatic.estore.textsearch.SearchConstants;

public interface AbcIndexBuildService
{
    public void buildIndex(SearchConstants.UPDATE_TYPE type, String hql);
    public void buildIndex();
}
