package hudson.plugins.spotinst.cloud;

import hudson.model.Label;

/**
 * Created by ohadmuchnik on 20/03/2017.
 */
public class ProvisionRequest {
    private String  label;
    private Integer executors;

    public ProvisionRequest(Label label, Integer executors) {
        if (label != null) {
            this.label = label.getName();
        }
        this.executors = executors;
    }

    public String getLabel() {
        return label;
    }

    public Integer getExecutors() {
        return executors;
    }

    public void setExecutors(Integer executors) {
        this.executors = executors;
    }
}
