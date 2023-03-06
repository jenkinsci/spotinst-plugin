package hudson.plugins.spotinst.model.common;

public class BlResponse<T> {

    //region Members
    private T       result;
    private boolean isSucceed;
    private String  errorMessage;
    //endregion

    //region Constructor
    public BlResponse() {
    }

    public BlResponse(T result) {
        this.isSucceed = true;
        this.result = result;
    }

    public BlResponse(boolean isSucceed) {
        this.isSucceed = isSucceed;
    }
    //endregion

    //region Getters & Setters
    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
        this.isSucceed = true;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSucceed() {
        return isSucceed;
    }

    public void setSucceed(boolean succeed) {
        isSucceed = succeed;
    }
    //endregion
}
