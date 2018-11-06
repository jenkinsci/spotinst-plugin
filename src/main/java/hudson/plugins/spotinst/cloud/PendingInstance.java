package hudson.plugins.spotinst.cloud;

import java.util.Date;

/**
 * Created by ohadmuchnik on 19/03/2017.
 */
public class PendingInstance {
    private String     id;
    private StatusEnum status;
    private Integer    numOfExecutors;
    private String     requestedLabel;
    private Date       createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public String getRequestedLabel() {
        return requestedLabel;
    }

    public void setRequestedLabel(String requestedLabel) {
        this.requestedLabel = requestedLabel;
    }

    public enum StatusEnum {
        PENDING("PENDING"),
        INSTANCE_INITIATING("INSTANCE_INITIATING");

        private String name;

        StatusEnum(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
