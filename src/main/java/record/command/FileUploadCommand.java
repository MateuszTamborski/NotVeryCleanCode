package record.command;

import record.service.impl.Constants;

public class FileUploadCommand {
    private String fee;
    private Integer totalRecords;

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public boolean hasFee(){
        if (fee.equalsIgnoreCase(Constants.YES)) {
            return true;
        }
        return false;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }
}
