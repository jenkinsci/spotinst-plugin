package hudson.plugins.spotinst.common;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.repos.ILockRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import jenkins.model.Jenkins;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

public class GroupLockingManager {
    //region constants
    public static final  Integer LOCK_TIME_TO_LIVE_IN_SECONDS                 = 60 * 3;
    private static final String  GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT =
            "group '%s' cannot be connected - check cloud's configuration";
    private static final String  LOCK_OK_STATUS                               = "OK";
    private static final Integer KEEP_ALIVE_MAXIMUM_ASSUMED_RUNNING_TIME      = 10;
    private static final Integer INITIALIZING_PERIOD_IN_SECONDS               =
            LOCK_TIME_TO_LIVE_IN_SECONDS + KEEP_ALIVE_MAXIMUM_ASSUMED_RUNNING_TIME;
    //endregion

    //region members
    private static final Logger    LOGGER                      = LoggerFactory.getLogger(GroupLockingManager.class);
    private static final String    currentControllerIdentifier = generateControllerIdentifier();
    private static final ILockRepo lockRepo                    = RepoManager.getInstance().getLockRepo();

    private final GroupLockKey                    key;
    private       SpotinstCloudCommunicationState cloudCommunicationState;
    private       String                          errorDescription;
    private       Date                            timeStamp;
    //endregion

    //region Constructor
    public GroupLockingManager(String groupId, String accountId) {
        key = new GroupLockKey(groupId, accountId);
        setInitializingState();
        String errMsg = null;

        if (StringUtils.isEmpty(groupId)) {
            errMsg = "Found a cloud with uninitialized Group ID. please check configuration";
        }
        else if (StringUtils.isEmpty(accountId)) {
            errMsg = String.format(
                    "Found a cloud with groupId '%s' and uninitialized Account ID. please check configuration",
                    groupId);
        }

        if (StringUtils.isNotEmpty(errMsg)) {
            setFailedState(errMsg);
        }
    }
    //endregion

    //region methods
    public void syncGroupController() {
        if (isActive()) {
            ApiResponse<String> getLockGroupRepoResponse =
                    lockRepo.getGroupControllerLockValue(getAccountId(), getGroupId());

            if (getLockGroupRepoResponse.isRequestSucceed()) {
                String  lockGroupControllerValue       = getLockGroupRepoResponse.getValue();
                boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

                if (isGroupAlreadyHasAnyController) {
                    boolean isGroupBelongToCurrentController =
                            StringUtils.equals(currentControllerIdentifier, lockGroupControllerValue);

                    if (isGroupBelongToCurrentController) {
                        SetGroupLockExpiry();
                    }
                    else {
                        handleGroupManagedByOtherController(lockGroupControllerValue);
                    }
                }
                else {
                    AcquireGroupLock();
                }
            }
            else {
                LOGGER.error("failed to get lock for groupId {}, accountId {}. Errors: {}", getGroupId(),
                             getAccountId(), getLockGroupRepoResponse.getErrors());

                if (cloudCommunicationState == SpotinstCloudCommunicationState.INITIALIZING) {
                    String failureDescription =
                            String.format(GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT, getGroupId());
                    handleInitializingFailureTimeout(failureDescription);
                }
            }
        }
    }

    public void deleteGroupControllerLock() {
        if (isActive()) {
            ApiResponse<String> getLockGroupRepoResponse =
                    lockRepo.getGroupControllerLockValue(getAccountId(), getGroupId());

            if (getLockGroupRepoResponse.isRequestSucceed()) {
                String lockGroupControllerValue = getLockGroupRepoResponse.getValue();
                deleteGroupLock(lockGroupControllerValue);
            }
            else {
                LOGGER.error("failed to get lock for groupId {} while unlocking. Errors: {}", getGroupId(),
                             getLockGroupRepoResponse.getErrors());
            }
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
        ApiResponse<String> lockGroupRepoResponse =
                lockRepo.acquireGroupControllerLock(getAccountId(), getGroupId(), currentControllerIdentifier,
                                                    LOCK_TIME_TO_LIVE_IN_SECONDS);

        if (lockGroupRepoResponse.isRequestSucceed()) {
            String  currentLock                  = lockGroupRepoResponse.getValue();
            boolean isLockedAcquiredSuccessfully = LOCK_OK_STATUS.equals(currentLock);

            if (isLockedAcquiredSuccessfully) {
                setReadyState();
            }
            else {
                String groupController;
                ApiResponse<String> getLockGroupRepoResponse =
                        lockRepo.getGroupControllerLockValue(getAccountId(), getGroupId());

                if (getLockGroupRepoResponse.isRequestSucceed()) {
                    groupController = getLockGroupRepoResponse.getValue();
                }
                else {
                    groupController = "Unknown";
                    LOGGER.error("failed to get group {} lock controller. Error: {}", getGroupId(),
                                 getLockGroupRepoResponse.getErrors());
                }

                handleGroupManagedByOtherController(groupController);
            }
        }
        else {
            LOGGER.error("Failed to acquire group lock for group {}, error description:{}", getGroupId(),
                         lockGroupRepoResponse.getErrors());

            if (cloudCommunicationState == SpotinstCloudCommunicationState.INITIALIZING) {
                String failureDescription = String.format(GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT, getGroupId());
                handleInitializingFailureTimeout(failureDescription);
            }
        }
    }

    private void SetGroupLockExpiry() {
        LOGGER.debug("group {} already belongs this controller, reviving the lock duration.", getGroupId());
        ApiResponse<String> response =
                lockRepo.setGroupControllerLockExpiry(getAccountId(), getGroupId(), currentControllerIdentifier,
                                                      LOCK_TIME_TO_LIVE_IN_SECONDS);
        if (response.isRequestSucceed()) {
            String  currentLock                  = response.getValue();
            boolean isLockedAcquiredSuccessfully = LOCK_OK_STATUS.equals(currentLock);

            if (isLockedAcquiredSuccessfully) {
                setReadyState();
            }
            else {
                LOGGER.error("Failed to revive the lock for group {} by the controller {}", getGroupId(),
                             currentControllerIdentifier);
            }
        }
        else {
            LOGGER.error("Failed to revive the lock for group {} by the controller {}, error description:{}",
                         getGroupId(), currentControllerIdentifier, response.getErrors());

        }
    }

    private void deleteGroupLock(String lockGroupController) {
        boolean isGroupAlreadyHasAnyController = lockGroupController != null;

        if (isGroupAlreadyHasAnyController) {
            boolean isGroupBelongToController = currentControllerIdentifier.equals(lockGroupController);

            if (isGroupBelongToController) {
                ApiResponse<Integer> deleteGroupRepoResponse =
                        lockRepo.deleteGroupControllerLock(getAccountId(), getGroupId());

                if (deleteGroupRepoResponse.isRequestSucceed()) {
                    LOGGER.info("Successfully unlocked group {}", getGroupId());
                }
                else {
                    LOGGER.error("Failed to unlock group {}. Errors: {}", getGroupId(),
                                 deleteGroupRepoResponse.getErrors());
                }
            }
            else {
                LOGGER.error("Controller {} could not unlock group {} - already locked by another Controller {}",
                             currentControllerIdentifier, getGroupId(), lockGroupController);
            }
        }
        else {
            LOGGER.error("Failed to unlock group {}. group is not locked.", getGroupId());
        }
    }

    private void handleGroupManagedByOtherController(String groupLockController) {
        String failureDescription =
                String.format("group '%s' is already connected to a different Jenkins controller %s", getGroupId(),
                              groupLockController);

        switch (cloudCommunicationState) {
            case INITIALIZING:
                handleInitializingFailureTimeout(failureDescription);
                break;

            case FAILED:
                setFailedState(failureDescription);
                break;

            case READY:
                LOGGER.warn("The group {} is in state ready but it belong to controller {}," +
                            " it may be because of jenkins plugin previous running(that saved in the jelly file and reloaded)," +
                            "returning to initializing state", getGroupId(), groupLockController);
                setInitializingState();
                break;
        }
    }

    private void handleInitializingFailureTimeout(String errorDescription) {
        boolean isTimeout = TimeUtils.isTimePassed(timeStamp, INITIALIZING_PERIOD_IN_SECONDS, Calendar.SECOND);

        if (isTimeout) {
            LOGGER.error("Initialization time has expired, error description: {}", errorDescription);
            setFailedState(errorDescription);
        }
        else {
            LOGGER.warn(
                    "failed to take control of the group, staying in initialize state until the time will expired, description: {}",
                    errorDescription);
        }
    }

    private void setInitializingState() {
        setCloudCommunicationState(SpotinstCloudCommunicationState.INITIALIZING);
        setErrorDescription(null);
        timeStamp = new Date();
    }

    private void setReadyState() {
        LOGGER.info("Successfully locked group {} controller", getGroupId());
        setCloudCommunicationState(SpotinstCloudCommunicationState.READY);
    }

    private void setFailedState(String description) {
        setCloudCommunicationState(SpotinstCloudCommunicationState.FAILED);
        setErrorDescription(description);
    }

    private static String generateControllerIdentifier() {
        String retVal;
        String hostName;

        try {
            hostName = java.net.InetAddress.getLocalHost().getHostAddress();
            Integer port = generateControllerPort();

            retVal = String.format("%s:%s", hostName, port);
        }
        catch (Exception exception) {
            retVal = RandomStringUtils.randomAlphanumeric(10);
            LOGGER.warn("Exception while getting hostname. generating random identifier '{}' instead. Exception {}",
                        retVal, exception);
        }

        return retVal;
    }

    private static Integer generateControllerPort() throws MalformedURLException {
        String fullControllerUrl = Jenkins.getInstance().getConfiguredRootUrl();
        int    retVal;

        URL urlHelper = new URL(fullControllerUrl);
        retVal = urlHelper.getPort();

        return retVal;
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
