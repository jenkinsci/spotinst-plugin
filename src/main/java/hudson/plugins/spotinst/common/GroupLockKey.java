package hudson.plugins.spotinst.common;

public class GroupLockKey {
    //region members
    private final String                          groupId;
    private final String                          accountId;
    //endregion

    //region Constructor
    public GroupLockKey(String groupId, String accountId) {
        this.groupId = groupId;
        this.accountId = accountId;
    }
    //endregion

    //region getters & setters
    public String getGroupId() {
        return groupId;
    }

    public String getAccountId() {
        return accountId;
    }
    //endregion
}
