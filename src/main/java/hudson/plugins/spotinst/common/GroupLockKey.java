package hudson.plugins.spotinst.common;

import org.apache.commons.lang.StringUtils;

public class GroupLockKey {
    //region members
    private final String groupId;
    private final String accountId;
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

    //region overrides
    @Override
    public boolean equals(Object other) {
        boolean retVal = false;

        if (this == other) {
            retVal = true;
        }
        else if (other != null) {
            if (other instanceof GroupLockKey) {
                boolean isEqualGroupIds   = StringUtils.equals(getGroupId(), ((GroupLockKey) other).getGroupId());
                boolean isEqualAccountIds = StringUtils.equals(getAccountId(), ((GroupLockKey) other).getAccountId());
                retVal = isEqualGroupIds && isEqualAccountIds;
            }
        }

        return retVal;
    }

    @Override
    public int hashCode() {
        int retVal = 0;

        if(groupId != null){
            retVal += groupId.hashCode();
        }

        if(accountId != null){
            retVal *= 37;
            retVal += accountId.hashCode();
        }

        return retVal;
    }
    //endregion
}
