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
    public static BlResponse<Boolean> AcquireLockGroupController(String accountId, String groupId,
                                                                 String controllerIdentifier) {
        BlResponse<Boolean> retVal = new BlResponse<>();
        Integer             ttl    = TimeHelper.getLockTimeToLeaveInSeconds();
        ApiResponse<String> lockGroupControllerResponse =
                lockRepo.acquireGroupControllerLock(accountId, groupId, controllerIdentifier, ttl);

        if (lockGroupControllerResponse.isRequestSucceed()) {
            String  responseValue                = lockGroupControllerResponse.getValue();
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
            String errorMessage =
                    String.format("lock request failed. Errors: %s", lockGroupControllerResponse.getErrors());
            LOGGER.error(errorMessage);
            retVal.setErrorMessage(errorMessage);
        }


        return retVal;
    }

    public static BlResponse<Boolean> SetGroupControllerLockExpiry(String accountId, String groupId,
                                                                   String controllerIdentifier) {
        BlResponse<Boolean> retVal = new BlResponse<>();
        Integer             ttl    = TimeHelper.getLockTimeToLeaveInSeconds();
        ApiResponse<String> lockGroupControllerResponse =
                lockRepo.setGroupControllerLockExpiry(accountId, groupId, controllerIdentifier, ttl);

        if (lockGroupControllerResponse.isRequestSucceed()) {
            String  responseValue                = lockGroupControllerResponse.getValue();
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
            String errorMessage = String.format("could not set lock expiry for group %s. Errors: %s", groupId,
                                                lockGroupControllerResponse.getErrors());
            LOGGER.error(errorMessage);
            retVal.setErrorMessage(errorMessage);
        }

        return retVal;
    }

    public static void UnlockGroups(Set<GroupLockKey> groupLockKeys) {
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
                            unlockGroup(groupLockKey);
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
    //endregion

    //region private methods
    private static void unlockGroup(GroupLockKey groupNoLongerExists) {
        String               groupId                      = groupNoLongerExists.getGroupId();
        String               accountId                    = groupNoLongerExists.getAccountId();
        ApiResponse<Integer> groupControllerValueResponse = lockRepo.deleteGroupControllerLock(accountId, groupId);

        if (groupControllerValueResponse.isRequestSucceed()) {
            LOGGER.info("Successfully unlocked group {}", groupId);
        }
        else {
            LOGGER.error("Failed to unlock group {}. Errors: {}", groupId, groupControllerValueResponse.getErrors());
        }
    }
    //endregion
}
