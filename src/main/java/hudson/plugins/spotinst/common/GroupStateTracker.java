package hudson.plugins.spotinst.common;

import java.util.Date;

import static hudson.plugins.spotinst.common.SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_INITIALIZING;

public class GroupStateTracker {
    //region members
    private final String                          groupId;
    private final String                          accountId;
    private       SpotinstCloudCommunicationState state;
    private final Date                            timeStamp;
    //endregion

    //region Constructor
    public GroupStateTracker(String groupId, String accountId) {
        this.groupId = groupId;
        this.accountId = accountId;
        state = SPOTINST_CLOUD_COMMUNICATION_INITIALIZING;
        timeStamp = new Date();
    }
    //endregion

    //region getters & setters
    public String getGroupId() {
        return groupId;
    }

    public String getAccountId() {
        return accountId;
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
    //endregion
}
