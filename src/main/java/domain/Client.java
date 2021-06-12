package domain;

import record.service.impl.Constants;

public class Client {

    private String counterTransfer;
    private Integer amountDivider;
    private String creditDebit;
    private Object profile;

    public String getCounterTransfer() {
        return counterTransfer;
    }

    public void setCounterTransfer(String counterTransfer) {
        this.counterTransfer = counterTransfer;
    }

    public Integer getAmountDivider() {
        return amountDivider;
    }

    public void setAmountDivider(Integer amountDivider) {
        this.amountDivider = amountDivider;
    }

    public String getCreditDebit() {
        return creditDebit;
    }

    public void setCreditDebit(String creditDebit) {
        this.creditDebit = creditDebit;
    }

    public Object getProfile() {
        return profile;
    }

    public void setProfile(Object profile) {
        this.profile = profile;
    }

    public boolean isBalanced() {
        return Constants.TRUE.equalsIgnoreCase(getCounterTransfer());
    }
}
