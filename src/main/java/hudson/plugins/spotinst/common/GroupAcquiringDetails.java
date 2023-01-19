package hudson.plugins.spotinst.common;

import java.util.Date;

import static hudson.plugins.spotinst.common.SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_INITIALIZING;

public class GroupAcquiringDetails {
    //region members
    private final GroupLockKey                    key;
    private       SpotinstCloudCommunicationState state;
    private final Date                            timeStamp;
    //endregion

    //region Constructor
    public GroupAcquiringDetails(String groupId, String accountId) {
        key = new GroupLockKey(groupId, accountId);
        state = SPOTINST_CLOUD_COMMUNICATION_INITIALIZING;
        timeStamp = new Date();
    }
    //endregion

    //region getters & setters
    public String getGroupId() {
        return key.getGroupId();
    }

    public String getAccountId() {
        return key.getAccountId();
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
