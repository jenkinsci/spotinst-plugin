package hudson.plugins.spotinst.common;

import hudson.plugins.spotinst.cloud.helpers.GroupLockHelper;
import hudson.plugins.spotinst.model.common.BlResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

public class GroupLockingManager {
    //region constants
    private static final Integer INITIALIZING_PERIOD                                 =
            Constants.SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS / Constants.MILI_TO_SECONDS;
    public static final  String  GROUP_LOCKED_BY_OTHER_CONTROLLER_DESCRIPTION_FORMAT =
            "%s is already connected to a different Jenkins controller";
    public static final  String  GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT        =
            "%s cannot be connected - check cloud's configuration";
    //endregion

    //region members
    private static final Logger                          LOGGER = LoggerFactory.getLogger(GroupLockingManager.class);
    private final        GroupLockKey                    key;
    private              SpotinstCloudCommunicationState cloudCommunicationState;
    private              String                          errorDescription;
    private              Date                            timeStamp;
    //endregion

    //region Constructor
    public GroupLockingManager(String groupId, String accountId) {
        key = new GroupLockKey(groupId, accountId);

        if (isActive()) {
            setInitializingState();
        }
        else {
            setFailedState("Account & Group ID must be initialized for all clouds");
        }
    }
    //endregion

    //region methods
    public void syncGroupController() {
        if (isActive()) {
            BlResponse<String> lockGroupControllerResponse =
                    GroupLockHelper.GetGroupControllerLock(getAccountId(), getGroupId());

            if (lockGroupControllerResponse.isSucceed()) {
                String  lockGroupControllerValue       = lockGroupControllerResponse.getResult();
                boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

                if (isGroupAlreadyHasAnyController) {
                    boolean isGroupBelongToCurrentController =
                            GroupLockHelper.isBelongToController(lockGroupControllerValue);

                    if (isGroupBelongToCurrentController) {
                        SetGroupLockExpiry();
                    }
                    else {
                        String failureDescription =
                                String.format(GroupLockingManager.GROUP_LOCKED_BY_OTHER_CONTROLLER_DESCRIPTION_FORMAT,
                                              getGroupId());
                        handleGroupDoesNotManagedByThisController(failureDescription);
                    }
                }
                else {
                    AcquireGroupLock();
                }
            }
            else {
                if (cloudCommunicationState == SpotinstCloudCommunicationState.INITIALIZING) {
                    String failureDescription =
                            String.format(GroupLockingManager.GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT,
                                          getGroupId());
                    handleInitializingExpired(failureDescription);
                }
            }
        }
    }

    public void deleteGroupControllerLock() {
        if (isActive()) {
            GroupLockHelper.DeleteGroupControllerLock(key);
        }
    }

    public boolean isCloudReadyForGroupCommunication() {
        boolean retVal = cloudCommunicationState == SpotinstCloudCommunicationState.READY;

        return retVal;
    }

    public boolean isActive() {
        boolean retVal = StringUtils.isNotEmpty(getAccountId()) && StringUtils.isNotEmpty(getGroupId());

        return retVal;
    }

    public boolean hasSameLock(GroupLockingManager other) {
        boolean retVal = false;

        if (other != null) {
            if (this == other) {
                retVal = true;
            }
            else {
                retVal = key.equals(other.key);
            }
        }

        return retVal;
    }
    //endregion

    //region private mehtods
    private void AcquireGroupLock() {
        LOGGER.info(String.format("group %s doesn't belong to any controller. trying to lock it", getGroupId()));
        BlResponse<Boolean> lockResponse = GroupLockHelper.AcquireGroupControllerLock(getAccountId(), getGroupId());
        handleLockResponse(lockResponse);
    }

    private void SetGroupLockExpiry() {
        LOGGER.info("group {} already belongs this controller, reviving the lock duration.", getGroupId());
        BlResponse<Boolean> lockResponse = GroupLockHelper.SetGroupControllerLockExpiry(getAccountId(), getGroupId());
        handleLockResponse(lockResponse);
    }

    private void handleGroupDoesNotManagedByThisController(String failureDescription) {
        LOGGER.warn("group {} does not belong to this controller", getGroupId());

        if (cloudCommunicationState == SpotinstCloudCommunicationState.INITIALIZING) {
            handleInitializingExpired(failureDescription);
        }
        else if (cloudCommunicationState == SpotinstCloudCommunicationState.READY) {
            setInitializingState();
        }
    }

    private void handleInitializingExpired(String description) {
        boolean shouldFail = TimeUtils.isTimePassed(timeStamp, INITIALIZING_PERIOD, Calendar.SECOND);

        if (shouldFail) {
            LOGGER.error(
                    "group {} belong to other controller, please make sure that there is no duplicated Jenkins controllers configured to the same group",
                    getGroupId());
            setFailedState(description);
        }
        else {
            LOGGER.warn("waiting to take control over group {}", getGroupId());
        }
    }

    private void handleLockResponse(BlResponse<Boolean> response) {
        if (response.isSucceed()) {
            Boolean hasLock = response.getResult();

            if (hasLock) {
                setReadyState();
            }
            else {
                String failureDescription =
                        String.format(GROUP_LOCKED_BY_OTHER_CONTROLLER_DESCRIPTION_FORMAT, getGroupId());
                handleGroupDoesNotManagedByThisController(failureDescription);
            }
        }
        else {
            if (cloudCommunicationState == SpotinstCloudCommunicationState.INITIALIZING) {
                String failureDescription = String.format(GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT, getGroupId());
                handleInitializingExpired(failureDescription);
            }
        }
    }

    private void setInitializingState() {
        setCloudCommunicationState(SpotinstCloudCommunicationState.INITIALIZING);
        setErrorDescription(null);
        timeStamp = new Date();
    }

    private void setReadyState() {
        setCloudCommunicationState(SpotinstCloudCommunicationState.READY);
    }

    private void setFailedState(String description) {
        setCloudCommunicationState(SpotinstCloudCommunicationState.FAILED);
        setErrorDescription(description);
    }
    //endregion

    //region getters & setters
    public String getGroupId() {
        return key.getGroupId();
    }

    public String getAccountId() {
        return key.getAccountId();
    }

    public SpotinstCloudCommunicationState getCloudCommunicationState() {
        return cloudCommunicationState;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    private void setCloudCommunicationState(SpotinstCloudCommunicationState cloudCommunicationState) {
        this.cloudCommunicationState = cloudCommunicationState;
    }

    private void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
    //endregion
}
