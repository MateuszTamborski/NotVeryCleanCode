package domain;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collection;

public class Record {
    private Currency currency;
    private Integer feeRecord;
    private String sign;
    private BigDecimal amount;
    private String beneficiaryName;
    private Bank bank;
    private String beneficiaryAccountNumber;
    private Integer counterTransfer;

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Integer getFeeRecord() {
        return feeRecord;
    }

    public void setFeeRecord(Integer feeRecord) {
        this.feeRecord = feeRecord;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public String getBeneficiaryAccountNumber() {
        return beneficiaryAccountNumber;
    }

    public void setBeneficiaryAccountNumber(String beneficiaryAccountNumber) {
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
    }

    public Comparable<Integer> getIsCounterTransferRecord() {
        return null;
    }

    public class Bank {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public boolean isCounterTransferRecord() {
        return Integer.valueOf(1).equals(getIsCounterTransferRecord());
    }

    public boolean hasFee() {
        return Integer.valueOf(1).equals(getIsCounterTransferRecord());
    }
}
