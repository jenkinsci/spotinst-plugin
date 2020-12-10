package hudson.plugins.spotinst.repos;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public class RepoManager {
    //region Members
    private IAwsGroupRepo     awsGroupRepo;
    private IGcpGroupRepo     gcpGroupRepo;
    private IAzureGroupRepo   azureGroupRepo;
    private IAzureVmGroupRepo azureV3GroupRepo;
    //endregion

    //region Constructor
    private RepoManager() {
        this.awsGroupRepo = new AwsGroupRepo();
        this.gcpGroupRepo = new GcpGroupRepo();
        this.azureGroupRepo = new AzureGroupRepo();
        this.azureV3GroupRepo = new AzureVmGroupRepo();
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

    public IAzureVmGroupRepo getAzureV3GroupRepo() {
        return azureV3GroupRepo;
    }

    public void setAzureV3GroupRepo(IAzureVmGroupRepo azureV3GroupRepo) {
        this.azureV3GroupRepo = azureV3GroupRepo;
    }
    //endregion
}
