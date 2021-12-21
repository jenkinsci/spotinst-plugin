package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.model.aws.AwsInstanceType;
import java.util.List;

/**
 * Created by Liron Arad on 08/12/2021.
 */
public class AwsInstanceTypesRepo implements IAwsInstanceTypesRepo {

    @Override
    public ApiResponse<List<AwsInstanceType>> getAllInstanceTypes(String accountId) {
        ApiResponse<List<AwsInstanceType>> retVal;

        try {
            List<AwsInstanceType> instances = SpotinstApi.getAllAwsInstanceTypes(accountId);

            retVal = new ApiResponse<>(instances);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
