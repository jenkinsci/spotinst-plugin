package hudson.plugins.spotinst.common;

import java.util.Date;

public class GroupStateTracker {
    //region members
    private String groupId;
    private String accountId;
    private SpotinstCloudCommunicationState state;
    private Date timeStamp;
    //endregion

    //region getters & setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public SpotinstCloudCommunicationState getState() {
        return state;
    }

    public void setState(SpotinstCloudCommunicationState state) {
        this.state = state;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
    //endregion
}
