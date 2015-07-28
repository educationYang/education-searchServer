package com.cartmatic.estoresearch.solr.index;

import java.util.List;

import com.cartmatic.estore.textsearch.SearchConstants;

public interface IndexBuildService
{
    public void buildIndex(SearchConstants.UPDATE_TYPE type, List<Integer> ids, String hql);
}
