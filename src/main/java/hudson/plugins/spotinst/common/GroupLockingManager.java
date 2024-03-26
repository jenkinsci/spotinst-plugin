package hudson.plugins.spotinst.common;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.repos.RepoManager;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class GroupLockingManager {
    //region constants
    public static final  Integer LOCK_TIME_TO_LIVE_IN_SECONDS                       = 60 * 3;
    private static final String  GROUP_CANNOT_BE_CONNECTED_DESCRIPTION_FORMAT       =
            "group '%s' cannot be connected - check cloud's configuration";
    private static final String  LOCK_OK_STATUS                                     = "OK";
    private static final Integer KEEP_ALIVE_MAXIMUM_ASSUMED_RUNNING_TIME_IN_SECONDS = 10;
    private static final Integer INITIALIZING_PERIOD_IN_SECONDS                     =
            LOCK_TIME_TO_LIVE_IN_SECONDS + KEEP_ALIVE_MAXIMUM_ASSUMED_RUNNING_TIME_IN_SECONDS;
    //endregion

    //region members
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupLockingManager.class);

    /*
    Jenkins ID. Must only be accesses only through the getter.
    Must be initialized lazily, cannot call JenkinsLocationConfiguration.get() before initialization is complete,
    Causing major UI bugs (defecting Jobs' pipelines loading)
    */
    private static String currentControllerIdentifier;

    private final GroupLockKey                    key;
    private       SpotinstCloudCommunicationState cloudCommunicationState;
    private       String                          errorDescription;
    private       Date                            initializingStateStartTimeStamp;
    //endregion

    //region Constructor
    public GroupLockingManager(String groupId, String accountId) {
        key = new GroupLockKey(groupId, accountId);
        boolean hasGroupId = isActive();

        if (hasGroupId) {
            setInitializingState();
        }
        else {
            setFailedState("Found a cloud with uninitialized Group ID. please check configuration");
        }
    }
    //endregion

    //region methods
    public void syncGroupController() {
        if (isActive()) {
            ApiResponse<String> getLockGroupRepoResponse =
                    RepoManager.getInstance().getLockRepo().getGroupControllerLockValue(getAccountId(), getGroupId());

            if (getLockGroupRepoResponse.isRequestSucceed()) {
                String  lockGroupControllerValue       = getLockGroupRepoResponse.getValue();
                boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

                if (isGroupAlreadyHasAnyController) {
                    boolean isGroupBelongToCurrentController =
                            StringUtils.equals(getCurrentControllerIdentifier(), lockGroupControllerValue);

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
                LOGGER.error("failed to get lock for groupId {}. Errors: {}", getGroupId(),
                             getLockGroupRepoResponse.getErrors());

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
                    RepoManager.getInstance().getLockRepo().getGroupControllerLockValue(getAccountId(), getGroupId());

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
        boolean retVal = StringUtils.isNotEmpty(getGroupId());

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
        ApiResponse<String> lockGroupRepoResponse = RepoManager.getInstance().getLockRepo()
                                                               .acquireGroupControllerLock(getAccountId(), getGroupId(),
                                                                                           getCurrentControllerIdentifier(),
                                                                                           LOCK_TIME_TO_LIVE_IN_SECONDS);

        if (lockGroupRepoResponse.isRequestSucceed()) {
            String  currentLock                  = lockGroupRepoResponse.getValue();
            boolean isLockedAcquiredSuccessfully = LOCK_OK_STATUS.equals(currentLock);

            if (isLockedAcquiredSuccessfully) {
                LOGGER.info("Successfully locked group {} controller", getGroupId());
                setReadyState();
            }
            else {
                String groupController;
                ApiResponse<String> getLockGroupRepoResponse = RepoManager.getInstance().getLockRepo()
                                                                          .getGroupControllerLockValue(getAccountId(),
                                                                                                       getGroupId());

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
        ApiResponse<String> response = RepoManager.getInstance().getLockRepo()
                                                  .setGroupControllerLockExpiry(getAccountId(), getGroupId(),
                                                                                getCurrentControllerIdentifier(),
                                                                                LOCK_TIME_TO_LIVE_IN_SECONDS);
        if (response.isRequestSucceed()) {
            String  currentLock                  = response.getValue();
            boolean isLockedAcquiredSuccessfully = LOCK_OK_STATUS.equals(currentLock);

            if (isLockedAcquiredSuccessfully) {
                setReadyState();
            }
            else {
                LOGGER.error("Failed to revive the lock for group {} by the controller {}", getGroupId(),
                             getCurrentControllerIdentifier());
            }
        }
        else {
            LOGGER.error("Failed to revive the lock for group {} by the controller {}, error description:{}",
                         getGroupId(), getCurrentControllerIdentifier(), response.getErrors());

        }
    }

    private void deleteGroupLock(String lockGroupController) {
        boolean isGroupAlreadyHasAnyController = lockGroupController != null;

        if (isGroupAlreadyHasAnyController) {
            boolean isGroupBelongToController = getCurrentControllerIdentifier().equals(lockGroupController);

            if (isGroupBelongToController) {
                ApiResponse<Integer> deleteGroupRepoResponse =
                        RepoManager.getInstance().getLockRepo().deleteGroupControllerLock(getAccountId(), getGroupId());

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
                             getCurrentControllerIdentifier(), getGroupId(), lockGroupController);
            }
        }
        else {
            LOGGER.info("Failed to unlock group {}. group is not locked.", getGroupId());
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
        boolean isTimeout =
                TimeUtils.isTimePassedInSeconds(initializingStateStartTimeStamp, INITIALIZING_PERIOD_IN_SECONDS);

        if (isTimeout) {
            String timeoutErrorDescription =
                    String.format("Initialization time has expired, error description: %s", errorDescription);
            setFailedState(timeoutErrorDescription);
        }
        else {
            LOGGER.warn(
                    "failed to take control of the group, staying in initialize state until the time will expired, description: {}",
                    errorDescription);
        }
    }

    public void setInitializingState() {
        setCloudCommunicationState(SpotinstCloudCommunicationState.INITIALIZING);
        setErrorDescription(null);
        initializingStateStartTimeStamp = new Date();
    }

    private void setReadyState() {
        setCloudCommunicationState(SpotinstCloudCommunicationState.READY);
    }

    public void setFailedState(String description) {
        LOGGER.error("Cloud failed to communicate with the group. {}", description);
        setCloudCommunicationState(SpotinstCloudCommunicationState.FAILED);
        setErrorDescription(description);
    }

    private static String generateControllerIdentifier() {
        String retVal;
        String hostName;

        try {
            hostName = java.net.InetAddress.getLocalHost().getHostAddress();
            Integer port = generateControllerPort();

            boolean isValidPort = port > 0;

            if (isValidPort == false) {
                throw new Exception("Cant get plugin controller port");
            }

            retVal = String.format("%s:%s", hostName, port);
            LOGGER.info("Generated Jenkins controller identifier: {}", retVal);
        }
        catch (Exception exception) {
            retVal = RandomStringUtils.randomAlphanumeric(10);
            LOGGER.warn(
                    "Exception while getting Controller identifier by host name and port. generating random identifier '{}' as fallback instead. Exception {}",
                    retVal, exception);
        }

        return retVal;
    }

    private static Integer generateControllerPort() throws MalformedURLException, NullPointerException {
        String                       fullControllerUrl;
        int                          retVal       = -1;
        JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
        fullControllerUrl = globalConfig.getUrl();

        if (fullControllerUrl != null) {
            URL urlHelper = new URL(fullControllerUrl);
            retVal = urlHelper.getPort();
        }

        return retVal;
    }
    //endregion

    //region getters & setters
    public static String getCurrentControllerIdentifier() {
        if (currentControllerIdentifier == null) {
            currentControllerIdentifier = generateControllerIdentifier();
        }

        return currentControllerIdentifier;
    }

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
