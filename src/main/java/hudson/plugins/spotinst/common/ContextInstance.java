package hudson.plugins.spotinst.common;

import java.util.Date;

/**
 * Created by ohadmuchnik on 05/07/2016.
 */
public class ContextInstance {
    private Integer numOfExecutors;
    private String label;
    private Date createdAt;

    public Integer getNumOfExecutors() {
        return numOfExecutors;
    }

    public void setNumOfExecutors(Integer numOfExecutors) {
        this.numOfExecutors = numOfExecutors;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
