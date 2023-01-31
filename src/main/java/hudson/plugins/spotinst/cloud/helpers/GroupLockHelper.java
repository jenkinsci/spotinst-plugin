package hudson.plugins.spotinst.cloud.helpers;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.common.GroupLockKey;
import hudson.plugins.spotinst.model.common.BlResponse;
import hudson.plugins.spotinst.repos.ILockRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupLockHelper {
    //region members
    private static final Logger    LOGGER                      = LoggerFactory.getLogger(GroupLockHelper.class);
    private static final ILockRepo lockRepo                    = RepoManager.getInstance().getLockRepo();
    private static final String    currentControllerIdentifier = RandomStringUtils.randomAlphanumeric(10);
    //endregion

    //region Methods
    public static BlResponse<String> GetGroupControllerLock(String accountId, String groupId) {
        ILockRepo           lockRepo              = RepoManager.getInstance().getLockRepo();
        ApiResponse<String> lockGroupRepoResponse = lockRepo.getGroupControllerLockValue(accountId, groupId);

        BlResponse<String> retVal;

        if (lockGroupRepoResponse.isRequestSucceed()) {
            retVal = new BlResponse<>(lockGroupRepoResponse.getValue());
        }
        else {
            String errorMessage =
                    String.format("group locking service failed to get lock for groupId %s, accountId %s. Errors: %s",
                                  groupId, accountId, lockGroupRepoResponse.getErrors());
            LOGGER.error(errorMessage);
            retVal = new BlResponse<>(false);
            retVal.setErrorMessage(errorMessage);
        }

        return retVal;
    }

    public static BlResponse<Boolean> AcquireGroupControllerLock(String accountId, String groupId) {
        Integer ttl = Constants.LOCK_TIME_TO_LIVE_IN_SECONDS;
        ApiResponse<String> lockGroupRepoResponse =
                lockRepo.acquireGroupControllerLock(accountId, groupId, currentControllerIdentifier, ttl);

        BlResponse<Boolean> retVal = handleApiLockResponse(lockGroupRepoResponse, groupId);

        return retVal;
    }

    public static BlResponse<Boolean> SetGroupControllerLockExpiry(String accountId, String groupId) {
        Integer ttl = Constants.LOCK_TIME_TO_LIVE_IN_SECONDS;
        ApiResponse<String> lockGroupRepoResponse =
                lockRepo.setGroupControllerLockExpiry(accountId, groupId, currentControllerIdentifier, ttl);

        BlResponse<Boolean> retVal = handleApiLockResponse(lockGroupRepoResponse, groupId);

        return retVal;
    }

    public static void DeleteGroupControllerLock(GroupLockKey lockKey) {
        String groupId   = lockKey.getGroupId();
        String accountId = lockKey.getAccountId();
        LOGGER.info("unlocking group {}.", groupId);
        BlResponse<String> lockGroupControllerResponse = GetGroupControllerLock(accountId, groupId);

        if (lockGroupControllerResponse.isSucceed()) {
            String  lockGroupControllerValue       = lockGroupControllerResponse.getResult();
            boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

            if (isGroupAlreadyHasAnyController) {
                boolean isGroupBelongToController = currentControllerIdentifier.equals(lockGroupControllerValue);

                if (isGroupBelongToController) {
                    ApiResponse<Integer> deleteGroupRepoResponse =
                            lockRepo.deleteGroupControllerLock(accountId, groupId);

                    if (deleteGroupRepoResponse.isRequestSucceed()) {
                        LOGGER.info("Successfully unlocked group {}", groupId);
                    }
                    else {
                        LOGGER.error("Failed to unlock group {}. Errors: {}", groupId,
                                     deleteGroupRepoResponse.getErrors());
                    }
                }
                else {
                    LOGGER.error("Controller {} could not unlock group {} - already locked by another Controller {}",
                                 currentControllerIdentifier, groupId, lockGroupControllerValue);
                }
            }
            else {
                LOGGER.error("Failed to unlock group {}. group is not locked.", groupId);
            }
        }
        else {
            LOGGER.error("group unlocking service failed to get lock for groupId {}, accountId {}. Errors: {}", groupId,
                         accountId, lockGroupControllerResponse.getErrorMessage());
        }
    }

    public static Boolean isBelongToController(String controllerIdentifier) {
        return StringUtils.equals(currentControllerIdentifier, controllerIdentifier);
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
