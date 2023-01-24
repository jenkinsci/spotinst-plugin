package hudson.plugins.spotinst.cloud.helpers;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.model.common.BlResponse;
import hudson.plugins.spotinst.repos.ILockRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupLockHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupLockHelper.class);

    public static BlResponse<Boolean> AcquireLockGroupController(String groupId, String accountId,
                                                                 String controllerIdentifier) {
        BlResponse<Boolean> retVal   = new BlResponse<>();
        ILockRepo           lockRepo = RepoManager.getInstance().getLockRepo();
        ApiResponse<String> lockGroupControllerResponse =
                lockRepo.acquireLockGroupController(groupId, accountId, controllerIdentifier,
                                                    Constants.LOCK_TIME_TO_LIVE_IN_SECONDS);

        if (lockGroupControllerResponse.isRequestSucceed()) {
            String responseValue = lockGroupControllerResponse.getValue();

            if (Constants.LOCK_OK_STATUS.equals(responseValue)) {
                LOGGER.info("Successfully locked group {} controller", groupId);
                retVal.setResult(true);
            }
            else {
                LOGGER.error("Failed locking group {} controller, got response {}", groupId, responseValue);
                retVal.setResult(false);
            }
        }
        else {
            LOGGER.error("lock request failed. Errors: {}", lockGroupControllerResponse.getErrors());
        }

        retVal.setSucceed(lockGroupControllerResponse.isRequestSucceed());
        return retVal;
    }

    public static BlResponse<Boolean> ExpandGroupControllerLock(String groupId, String accountId,
                                                                String controllerIdentifier) {
        BlResponse<Boolean> retVal   = new BlResponse<>();
        ILockRepo           lockRepo = RepoManager.getInstance().getLockRepo();
        ApiResponse<String> lockGroupControllerResponse =
                lockRepo.expandGroupControllerLock(groupId, accountId, controllerIdentifier,
                                                   Constants.LOCK_TIME_TO_LIVE_IN_SECONDS);

        if (lockGroupControllerResponse.isRequestSucceed()) {
            String responseValue = lockGroupControllerResponse.getValue();

            if (Constants.LOCK_OK_STATUS.equals(responseValue)) {
                LOGGER.info("Successfully locked group {} controller", groupId);
                retVal.setResult(true);
            }
            else {
                LOGGER.error("Failed locking group {} controller, got response {}", groupId, responseValue);
                retVal.setResult(false);
            }
        }
        else {
            LOGGER.error("lock request failed. Errors: {}", lockGroupControllerResponse.getErrors());
        }

        retVal.setSucceed(lockGroupControllerResponse.isRequestSucceed());
        return retVal;
    }

}
