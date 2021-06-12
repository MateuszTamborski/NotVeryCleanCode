package confirmationLetter;

import dao.CurrencyDao;
import domain.BatchTotal;
import domain.Client;
import domain.HashBatchRecordsBalance;
import domain.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import record.service.impl.Constants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
class ConfirmationLetterGeneratorTest {

    private ConfirmationLetterGenerator confirmationLetterGenerator;
    private HashBatchRecordsBalance recordsBalance;
    private Client client;

    private BatchTotal createBatchTotal(int number, String sign) {
        BatchTotal bt = new BatchTotal();
        bt.setTransactionSign(sign);
        if (Constants.CREDIT.equals(sign)) {
            bt.setCreditValue(new BigDecimal(number));
        } else if (Constants.DEBIT.equals(sign)) {
            bt.setDebitValue(new BigDecimal(number));
        }
        return bt;
    }

    @BeforeEach
    public void setUp()
    {
        confirmationLetterGenerator = new ConfirmationLetterGenerator();
        recordsBalance = new HashBatchRecordsBalance();
        client = new Client();
    }

    /** It is just an example of how you can write tests in Junit. */
    @Test
    public void exampleOfTestInJunit()
    {
        CurrencyDao currencyDao = new CurrencyDao();
        confirmationLetterGenerator.setCurrencyDao(currencyDao);

        assertEquals(currencyDao, confirmationLetterGenerator.getCurrencyDao());
    }

    @Test
    public void testCreditBatchTotal_divider_one_all_credit(){
        BatchTotal one = new BatchTotal();
        one.setCreditValue(BigDecimal.ONE);
        BatchTotal ten = new BatchTotal();
        ten.setCreditValue(BigDecimal.TEN);

        Collection<BatchTotal> batchTotals = new ArrayList<>();
        batchTotals.add(one);
        batchTotals.add(ten);
        recordsBalance.setBatchTotals(batchTotals);

        BigDecimal result = recordsBalance.calculateTotalOverBatches(1, Constants.CREDIT);
        assertEquals(new BigDecimal(11), result);
    }

    @Test
    public void testCreditBatchTotal_divider_one_mixed_credit_debit() {
        Collection<BatchTotal> batchTotals = new ArrayList<>();
        batchTotals.add(createBatchTotal(1, Constants.CREDIT));
        batchTotals.add(createBatchTotal(10, Constants.CREDIT));
        batchTotals.add(createBatchTotal(5, Constants.DEBIT));
        recordsBalance.setBatchTotals(batchTotals);

        assertEquals(new BigDecimal(11), recordsBalance.calculateTotalOverBatches(1, Constants.CREDIT));
    }

    @Test
    public void testIsBalanced_TRUE() {
        client.setCounterTransfer(Constants.TRUE);
        assert(client.isBalanced());
    }

    @Test
    public void testIsBalanced_FALSE() {
        client.setCounterTransfer("FALSE");
        assert(!client.isBalanced());
    }

    @Test
    public void testIsBalanced_OTHER() {
        client.setCounterTransfer("GERAG#$");
        assert(!client.isBalanced());
    }
}