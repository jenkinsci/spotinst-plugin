package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.aws.AwsInstanceType;
import java.util.List;

/**
 * Created by Liron Arad on 08/12/2021.
 */
public interface IAwsInstanceTypesRepo {
    ApiResponse<List<AwsInstanceType>> getAllInstanceTypes(String accountId);
}
