package hudson.plugins.spotinst.cloud.helpers;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.common.GroupLockKey;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.model.common.BlResponse;
import hudson.plugins.spotinst.repos.ILockRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class GroupLockHelper {
    //region members
    private static final Logger    LOGGER   = LoggerFactory.getLogger(GroupLockHelper.class);
    private static final ILockRepo lockRepo = RepoManager.getInstance().getLockRepo();
    //endregion

    //region Methods
    public static BlResponse<String> GetGroupControllerLock(String accountId, String groupId) {
        ILockRepo           lockRepo                    = RepoManager.getInstance().getLockRepo();
        ApiResponse<String> lockGroupControllerResponse = lockRepo.getGroupControllerLockValue(accountId, groupId);

        BlResponse<String> retVal;

        if (lockGroupControllerResponse.isRequestSucceed()) {
            retVal = new BlResponse<>(lockGroupControllerResponse.getValue());
        }
        else {
            String errorMessage =
                    String.format("group locking service failed to get lock for groupId %s, accountId %s. Errors: %s",
                                  groupId, accountId, lockGroupControllerResponse.getErrors());
            LOGGER.error(errorMessage);
            retVal = new BlResponse<>(false);
            retVal.setErrorMessage(errorMessage);
        }

        return retVal;
    }

    public static BlResponse<Boolean> AcquireGroupControllerLock(String accountId, String groupId,
                                                                 String controllerIdentifier) {
        Integer ttl = Constants.LOCK_TIME_TO_LIVE_IN_SECONDS;
        ApiResponse<String> lockGroupControllerResponse =
                lockRepo.acquireGroupControllerLock(accountId, groupId, controllerIdentifier, ttl);

        BlResponse<Boolean> retVal = handleApiLockResponse(lockGroupControllerResponse, groupId);

        return retVal;
    }

    public static BlResponse<Boolean> SetGroupControllerLockExpiry(String accountId, String groupId,
                                                                   String controllerIdentifier) {
        Integer ttl = Constants.LOCK_TIME_TO_LIVE_IN_SECONDS;
        ApiResponse<String> lockGroupControllerResponse =
                lockRepo.setGroupControllerLockExpiry(accountId, groupId, controllerIdentifier, ttl);

        BlResponse<Boolean> retVal = handleApiLockResponse(lockGroupControllerResponse, groupId);

        return retVal;
    }

    public static void DeleteGroupControllerLocks(Set<GroupLockKey> groupLockKeys) {
        for (GroupLockKey groupLockKey : groupLockKeys) {
            String  groupId       = groupLockKey.getGroupId();
            String  accountId     = groupLockKey.getAccountId();
            boolean isActiveCloud = StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId);

            if (isActiveCloud) {
                LOGGER.info("unlocking group {}.", groupId);
                ApiResponse<String> lockGroupControllerResponse =
                        lockRepo.getGroupControllerLockValue(accountId, groupId);

                if (lockGroupControllerResponse.isRequestSucceed()) {
                    String  lockGroupControllerValue       = lockGroupControllerResponse.getValue();
                    boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

                    if (isGroupAlreadyHasAnyController) {
                        String  controllerIdentifier      = SpotinstContext.getInstance().getControllerIdentifier();
                        boolean isGroupBelongToController = controllerIdentifier.equals(lockGroupControllerValue);

                        if (isGroupBelongToController) {
                            deleteGroupControllerLock(groupLockKey);
                        }
                        else {
                            LOGGER.error(
                                    "Controller {} could not unlock group {} - already locked by another Controller {}",
                                    controllerIdentifier, groupId, lockGroupControllerValue);
                        }
                    }
                    else {
                        LOGGER.error("Failed to unlock group {}. group is not locked.", groupId);
                    }
                }
                else {
                    LOGGER.error("group unlocking service failed to get lock for groupId {}, accountId {}. Errors: {}",
                                 groupId, accountId, lockGroupControllerResponse.getErrors());
                }
            }
        }
    }

    public static void deleteGroupControllerLock(GroupLockKey lockKey) {
        String               groupId                      = lockKey.getGroupId();
        String               accountId                    = lockKey.getAccountId();
        ApiResponse<Integer> groupControllerValueResponse = lockRepo.deleteGroupControllerLock(accountId, groupId);

        if (groupControllerValueResponse.isRequestSucceed()) {
            LOGGER.info("Successfully unlocked group {}", groupId);
        }
        else {
            LOGGER.error("Failed to unlock group {}. Errors: {}", groupId, groupControllerValueResponse.getErrors());
        }
    }
    //endregion

    //region private methods
    private static BlResponse<Boolean> handleApiLockResponse(ApiResponse<String> apiResponse, String groupId) {
        BlResponse<Boolean> retVal = new BlResponse<>();

        if (apiResponse.isRequestSucceed()) {
            String  responseValue                = apiResponse.getValue();
            boolean isLockedAcquiredSuccessfully = Constants.LOCK_OK_STATUS.equals(responseValue);
            retVal.setResult(isLockedAcquiredSuccessfully);

            if (isLockedAcquiredSuccessfully) {
                LOGGER.info("Successfully locked group {} controller", groupId);
            }
            else {
                String errorMessage =
                        String.format("Failed locking group %s controller, already locked by another controller",
                                      groupId);
                LOGGER.error(errorMessage);
                retVal.setErrorMessage(errorMessage);
            }
        }
        else {
            retVal.setSucceed(false);
            String errorMessage = String.format("lock request failed. Errors: %s", apiResponse.getErrors());
            LOGGER.error(errorMessage);
            retVal.setErrorMessage(errorMessage);
        }

        return retVal;
    }
    //endregion
}
