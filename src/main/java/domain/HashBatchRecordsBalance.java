package domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HashBatchRecordsBalance {

    private BigDecimal hashTotalCredit;
    private BigDecimal hashTotalDebit;
    private Collection<BatchTotal> batchTotals;
    private BigDecimal totalFee;
    private String collectionType;

    public HashBatchRecordsBalance() {
        hashTotalCredit = BigDecimal.ZERO;
        hashTotalDebit = BigDecimal.ZERO;
        batchTotals = new ArrayList<>();
        totalFee = BigDecimal.ZERO;
        collectionType = "";
    }

    public BigDecimal getHashTotalCredit() {
        return hashTotalCredit;
    }

    public void setHashTotalCredit(BigDecimal hashTotalCredit) {
        this.hashTotalCredit = hashTotalCredit;
    }

    public BigDecimal getHashTotalDebit() {
        return hashTotalDebit;
    }

    public void setHashTotalDebit(BigDecimal hashTotalDebit) {
        this.hashTotalDebit = hashTotalDebit;
    }

    public void setBatchTotals(Collection<BatchTotal> batchTotals) {
        this.batchTotals = batchTotals;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    public Collection<BatchTotal> getBatchTotals() {
        return batchTotals;
    }

    public Integer getRecordsTotal()
    {
        return batchTotals.size();
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public BigDecimal calculateTotalOverBatches(Integer amountDivider, String sign) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BatchTotal batchTotal : batchTotals) {
            sum = sum.add(batchTotal.getTotalForSign(sign));
        }
        return sum.divide(new BigDecimal(amountDivider));
    }
}
