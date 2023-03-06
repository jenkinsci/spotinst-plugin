package hudson.plugins.spotinst.repos;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public class RepoManager {
    //region Members
    private IAwsGroupRepo         awsGroupRepo;
    private IGcpGroupRepo         gcpGroupRepo;
    private IAzureGroupRepo       azureGroupRepo;
    private IAzureVmGroupRepo     azureVmGroupRepo;
    private IAwsInstanceTypesRepo awsInstanceTypesRepo;
    private ILockRepo             lockRepo;
    //endregion

    //region Constructor
    private RepoManager() {
        this.awsGroupRepo = new AwsGroupRepo();
        this.gcpGroupRepo = new GcpGroupRepo();
        this.azureGroupRepo = new AzureGroupRepo();
        this.azureVmGroupRepo = new AzureVmGroupRepo();
        this.awsInstanceTypesRepo = new AwsInstanceTypesRepo();
        this.lockRepo = new LockRepo();
    }

    private static RepoManager instance = new RepoManager();
    //endregion

    //region Public Methods
    public static RepoManager getInstance() {
        return instance;
    }

    public IAwsGroupRepo getAwsGroupRepo() {
        return awsGroupRepo;
    }

    public void setAwsGroupRepo(IAwsGroupRepo awsGroupRepo) {
        this.awsGroupRepo = awsGroupRepo;
    }

    public IGcpGroupRepo getGcpGroupRepo() {
        return gcpGroupRepo;
    }

    public void setGcpGroupRepo(IGcpGroupRepo gcpGroupRepo) {
        this.gcpGroupRepo = gcpGroupRepo;
    }

    public IAzureGroupRepo getAzureGroupRepo() {
        return azureGroupRepo;
    }

    public void setAzureGroupRepo(IAzureGroupRepo azureGroupRepo) {
        this.azureGroupRepo = azureGroupRepo;
    }

    public IAzureVmGroupRepo getAzureVmGroupRepo() {
        return azureVmGroupRepo;
    }

    public void setAzureVmGroupRepo(IAzureVmGroupRepo azureVmGroupRepo) {
        this.azureVmGroupRepo = azureVmGroupRepo;
    }

    public IAwsInstanceTypesRepo getAwsInstanceTypesRepo() {
        return awsInstanceTypesRepo;
    }

    public void setAwsInstanceTypesRepo(IAwsInstanceTypesRepo awsInstanceTypesRepo) {
        this.awsInstanceTypesRepo = awsInstanceTypesRepo;
    }

    public ILockRepo getLockRepo() {
        return lockRepo;
    }

    public void setLockRepo(ILockRepo lockRepo) {
        this.lockRepo = lockRepo;
    }
    //endregion
}
