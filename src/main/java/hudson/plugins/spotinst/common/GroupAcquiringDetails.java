package hudson.plugins.spotinst.common;

import hudson.plugins.spotinst.cloud.helpers.GroupLockHelper;
import hudson.plugins.spotinst.model.common.BlResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

import static hudson.plugins.spotinst.common.SpotinstCloudCommunicationState.*;

public class GroupAcquiringDetails {
    //region constants
    private static final Integer INITIALIZING_PERIOD                                 =
            Constants.SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS / Constants.MILI_TO_SECONDS;
    public static final  String  GROUP_LOCKED_BY_OTHER_CONTROLLER_DESCRIPTION_FORMAT =
            "%s is already connected to a different Jenkins controller";
    public static final  String  GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT        =
            "%s cannot be connected - check cloud's configuration";
    //endregion

    //region members
    private static final Logger                          LOGGER = LoggerFactory.getLogger(GroupAcquiringDetails.class);
    private final        GroupLockKey                    key;
    private              SpotinstCloudCommunicationState state;
    private              String                          description;
    private              Date                            timeStamp;
    //endregion

    //region Constructor
    public GroupAcquiringDetails(String groupId, String accountId) {
        key = new GroupLockKey(groupId, accountId);

        if (isActive()) {
            setInitializingState();
        }
        else {
            setFailedState("Account & Group ID must be initialized for all clouds");
        }
    }
    //endregion

    //region mehtods
    public void syncGroupOwner() {
        BlResponse<String> lockGroupControllerResponse =
                GroupLockHelper.GetGroupControllerLock(getAccountId(), getGroupId());

        if (lockGroupControllerResponse.isSucceed()) {
            String  lockGroupControllerValue       = lockGroupControllerResponse.getResult();
            String  currentControllerIdentifier    = SpotinstContext.getInstance().getControllerIdentifier();
            boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

            if (isGroupAlreadyHasAnyController) {
                boolean isGroupBelongToCurrentController = currentControllerIdentifier.equals(lockGroupControllerValue);

                if (isGroupBelongToCurrentController) {
                    SetGroupLockExpiry(currentControllerIdentifier);
                }
                else {
                    LOGGER.warn(
                            "group {} does not belong to controller with identifier {}, make sure that there is no duplicated Jenkins controllers configured to the same group",
                            getGroupId(), currentControllerIdentifier);
                    handleGroupManagedByOtherController(
                            String.format(GroupAcquiringDetails.GROUP_LOCKED_BY_OTHER_CONTROLLER_DESCRIPTION_FORMAT,
                                          getGroupId()));
                }
            }
            else {
                AcquireGroupLock(currentControllerIdentifier);
            }
        }
        else {
            handleInitializingExpired(
                    String.format(GroupAcquiringDetails.GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT, getGroupId()));
        }
    }

    public boolean isCloudReadyForGroupCommunication() {
        boolean retVal = state == READY;

        return retVal;
    }

    public boolean isActive() {
        boolean retVal = StringUtils.isNotEmpty(getAccountId()) && StringUtils.isNotEmpty(getGroupId());

        return retVal;
    }
    //endregion

    //region private mehtods
    private void AcquireGroupLock(String controllerIdentifier) {
        LOGGER.info(String.format(
                "group %s doesn't belong to any controller. controller with identifier %s is trying to lock it",
                getGroupId(), controllerIdentifier));
        BlResponse<Boolean> lockResponse =
                GroupLockHelper.AcquireGroupControllerLock(getAccountId(), getGroupId(), controllerIdentifier);
        handleLockResponse(lockResponse);
    }

    private void SetGroupLockExpiry(String controllerIdentifier) {
        LOGGER.info("group {} already belongs the controller {}, reviving the lock duration.", getGroupId(),
                    controllerIdentifier);
        BlResponse<Boolean> lockResponse =
                GroupLockHelper.SetGroupControllerLockExpiry(getAccountId(), getGroupId(), controllerIdentifier);
        handleLockResponse(lockResponse);
    }

    private void handleGroupManagedByOtherController(String description) {
        handleInitializingExpired(description);

        if (state == READY) {
            setInitializingState();
        }
    }

    private void handleInitializingExpired(String description) {
        if (state == INITIALIZING) {

            boolean shouldFail = TimeUtils.isTimePassed(timeStamp, INITIALIZING_PERIOD, Calendar.SECOND);

            if (shouldFail) {
                setFailedState(description);
            }
        }
    }

    private void handleLockResponse(BlResponse<Boolean> response) {
        if (response.isSucceed()) {
            Boolean hasLock = response.getResult();

            if (hasLock) {
                setReadyState();
            }
            else {
                handleGroupManagedByOtherController(
                        String.format(GROUP_LOCKED_BY_OTHER_CONTROLLER_DESCRIPTION_FORMAT, getGroupId()));
            }
        }
        else {
            handleInitializingExpired(String.format(GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT, getGroupId()));
        }
    }

    private void setInitializingState() {
        setState(INITIALIZING);
        setDescription(null);
        timeStamp = new Date();
    }

    private void setReadyState() {
        setState(READY);
    }

    private void setFailedState(String description) {
        setState(FAILED);
        setDescription(description);
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

    public String getDescription() {
        return description;
    }

    private void setState(SpotinstCloudCommunicationState state) {
        this.state = state;
    }

    private void setDescription(String description) {
        this.description = description;
    }
    //endregion
}
