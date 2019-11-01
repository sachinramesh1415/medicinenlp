package com.mavenproject.domain;

import java.util.HashMap;
import java.util.Map;

public class Output {
    private String domain;
    private String query;
    private String queryResult;
    private Map<String, String> constraints;
    public Output() {
        constraints = new HashMap<String, String>();
    }
    public String getDomain() {
        return domain;
    }
    public void setDomain(String domain) {
        this.domain = domain;
    }
    public String getQueryResult() {
        return queryResult;
    }
    public void setQueryResult(String queryResult) {
        this.queryResult = queryResult;
    }
    public void setConstraint(String key, String value)
    {
        this.constraints.put(key,value);
    }
    public Map<String, String> getConstraints() {
        return constraints;
    }
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query;
    }
}
