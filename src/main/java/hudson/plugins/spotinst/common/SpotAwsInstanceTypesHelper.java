package hudson.plugins.spotinst.common;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.aws.AwsInstanceType;
import hudson.plugins.spotinst.repos.IAwsInstanceTypesRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Liron Arad on 08/12/2021.
 */
public class SpotAwsInstanceTypesHelper {

    //region Members
    private static final Logger LOGGER                                             =
            LoggerFactory.getLogger(SpotAwsInstanceTypesHelper.class);
    private static final long   AWS_INSTANCE_TYPES_UP_TO_DATE_THRESHOLD_IN_MINUTES = 60;
    private static final Object lockObject = new Object();
    //endregion

    public static List<AwsInstanceType> getAllInstanceTypes() {
        List<AwsInstanceType> retVal;

        synchronized (lockObject) {
            boolean needToLoadInstanceTypes = isInstanceTypesListUpdate() == false;

            if (needToLoadInstanceTypes == true) {
                List<AwsInstanceType> awsInstanceTypes;

                String                accountId        = SpotinstContext.getInstance().getAccountId();
                IAwsInstanceTypesRepo instanceTypeRepo = RepoManager.getInstance().getAwsInstanceTypesRepo();
                ApiResponse<List<AwsInstanceType>> allInstanceTypesResponse = instanceTypeRepo.getAllInstanceTypes(accountId);
                boolean isRequestSucceed = allInstanceTypesResponse.isRequestSucceed();

                if (isRequestSucceed) {
                    awsInstanceTypes = allInstanceTypesResponse.getValue();
                    Date now = new Date();
                    SpotinstContext.getInstance().setAwsInstanceTypesLastUpdate(now);
                    String massage = "instance types loaded using API call";

                    LOGGER.info(massage);
                    SpotinstContext.getInstance().setAwsInstanceTypes(awsInstanceTypes);
                }
                else {
                    if (SpotinstContext.getInstance().getAwsInstanceTypes() == null) {
                        awsInstanceTypes = getConstantInstanceTypesList();
                        String massage =
                                "Loading AWS instance types with an API call failed, using %s constant instance types.";
                        String massageWithListSize = String.format(massage, awsInstanceTypes.size());

                        LOGGER.error(massageWithListSize);
                        SpotinstContext.getInstance().setAwsInstanceTypes(awsInstanceTypes);
                    }
                }

            }

            retVal = SpotinstContext.getInstance().getAwsInstanceTypes();
        }
        return retVal;
    }

    private static List<AwsInstanceType> getConstantInstanceTypesList() {
        List<AwsInstanceType> retVal = new ArrayList<>();

        for (AwsInstanceTypeEnum instanceTypeEnum : AwsInstanceTypeEnum.values()) {
            String          type         = instanceTypeEnum.getValue();
            Integer         cpus         = instanceTypeEnum.getExecutors();
            AwsInstanceType instanceType = new AwsInstanceType();
            instanceType.setInstanceType(type);
            instanceType.setvCPU(cpus);
            retVal.add(instanceType);
        }

        return retVal;
    }

    public static boolean isInstanceTypesListUpdate() {
        boolean retVal;
        synchronized (lockObject) {

            Date awsInstanceTypesLastUpdate = SpotinstContext.getInstance().getAwsInstanceTypesLastUpdate();

            if (awsInstanceTypesLastUpdate == null) {
                retVal = false;
            }
            else {
                Date now               = new Date();
                long differentInMinute = TimeUtils.getDiffInMinutes(now, awsInstanceTypesLastUpdate);

                if (differentInMinute < AWS_INSTANCE_TYPES_UP_TO_DATE_THRESHOLD_IN_MINUTES) {
                    retVal = true;
                }
                else {
                    retVal = false;
                }
            }
        }
        return retVal;
    }
}
