package confirmationLetter;

import dao.CurrencyDao;
import domain.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import record.command.FileUploadCommand;
import record.domain.TempRecord;
import record.parser.FileExtension;
import record.service.impl.Constants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfirmationLetterGenerator {

    private static Log logger = LogFactory.getLog(ConfirmationLetterGenerator.class);

    private String crediting;
    private String debiting;
    private String debit;
    private String credit;
    private String type;
    private LetterSelector letterSelector;
    private CurrencyDao currencyDao;

    public CurrencyDao getCurrencyDao() {
        return currencyDao;
    }

    public void setCurrencyDao(CurrencyDao currencyDao) {
        this.currencyDao = currencyDao;
    }

    public OurOwnByteArrayOutputStream letter(RequestContext context,
                                              FileUploadCommand fileUploadCommand, Client client,
                                              HashBatchRecordsBalance hashBatchRecordsBalance, String branchName,
                                              List<AmountAndRecordsPerBank> bankMap,
                                              List<FaultRecord> faultyRecords,
                                              FileExtension extension, List<Record> records,
                                              List<TempRecord> faultyAccountNumberRecordList,
                                              List<TempRecord> sansDuplicateFaultRecordsList
    ) {

        ConfirmationLetter letter = createConfirmationLetter(fileUploadCommand, client, hashBatchRecordsBalance,
                branchName, bankMap, faultyRecords, extension, records, faultyAccountNumberRecordList,
                sansDuplicateFaultRecordsList);

        OurOwnByteArrayOutputStream arrayOutputStream = generateConfirmationLetterAsPDF(client, letter);

        context.getConversationScope().asMap().put("dsbByteArrayOutputStream", arrayOutputStream);

        return arrayOutputStream;
    }

    private OurOwnByteArrayOutputStream generateConfirmationLetterAsPDF(Client client, ConfirmationLetter letter) {
        OurOwnByteArrayOutputStream arrayOutputStream = letterSelector
                .generateLetter(client.getCreditDebit(), letter);
        return arrayOutputStream;
    }

    private ConfirmationLetter createConfirmationLetter(FileUploadCommand fileUploadCommand, Client client,
                                                        HashBatchRecordsBalance hashBatchRecordsBalance,
                                                        String branchName, List<AmountAndRecordsPerBank> bankMap,
                                                        List<FaultRecord> faultyRecords, FileExtension extension,
                                                        List<Record> records, List<TempRecord> faultyAccountNumberRecordList,
                                                        List<TempRecord> sansDuplicateFaultRecordsList) {

        ConfirmationLetter letter = new ConfirmationLetter();

        letter.setCurrency(records.get(0).getCurrency());
        letter.setExtension(extension);

        letter.setHashTotalCredit(hashBatchRecordsBalance.getHashTotalCredit().toString());
        letter.setHashTotalDebit(hashBatchRecordsBalance.getHashTotalDebit().toString());

        letter.setBatchTotalDebit(hashBatchRecordsBalance.calculateTotalOverBatches(client.getAmountDivider(),
                Constants.DEBIT).toString());
        letter.setBatchTotalCredit(hashBatchRecordsBalance.calculateTotalOverBatches(client.getAmountDivider(),
                Constants.CREDIT).toString());

        letter.setTotalProcessedRecords(hashBatchRecordsBalance.getRecordsTotal().toString());

        letter.setTransferType(hashBatchRecordsBalance.getCollectionType());
        letter.setBanks(bankMap);

        letter.setCreditingErrors(faultyRecords);
        letter.setClient(client);
        letter.setBranchName(branchName);

        letter.setTransactionCost(getTransactionCost(fileUploadCommand, hashBatchRecordsBalance));


        // uncommented this line

        Map<String, BigDecimal> retrievedAmounts = new HashMap<String, BigDecimal>();
        retrievedAmounts = calculateRetrieveAmounts(records, faultyRecords,
                client, extension, faultyAccountNumberRecordList,
                sansDuplicateFaultRecordsList);

        letter.setRetrievedAmountEur(retrievedAmounts.get(Constants.CURRENCY_EURO));
        letter.setRetrievedAmountFL(retrievedAmounts.get(Constants.CURRENCY_FL));
        letter.setRetrievedAmountUsd(retrievedAmounts.get(Constants.CURRENCY_FL));
        letter.setTotalRetrievedRecords(fileUploadCommand.getTotalRecords());
        return letter;
    }

    private String getTransactionCost(FileUploadCommand fileUploadCommand, HashBatchRecordsBalance hashBatchRecordsBalance) {
        String transactionCost = "";
        if (fileUploadCommand.hasFee()) {
            transactionCost = hashBatchRecordsBalance.getTotalFee().toString();
        }
        return transactionCost;
    }


    // Calculate sum amount from faultyAccountnumber list

    private Map<String, BigDecimal> calculateAmountsFaultyAccountNumber(
            List<TempRecord> faultyAccountNumberRecordList, Client client) {
        Map<String, BigDecimal> retrievedAmountsFaultyAccountNumber = new HashMap<String, BigDecimal>();

        BigDecimal faultyAccRecordAmountCreditFL = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountCreditUSD = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountCreditEUR = new BigDecimal(0);

        BigDecimal faultyAccRecordAmountDebitFL = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountDebitUSD = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountDebitEUR = new BigDecimal(0);

        for (TempRecord faultyAccountNumberRecord : faultyAccountNumberRecordList) {
            // // logger.debug("faultyAccountNumberRecord: "+
            // faultyAccountNumberRecord);
            // FL
            if (StringUtils.isBlank(faultyAccountNumberRecord.getSign())) {
                faultyAccountNumberRecord.setSign(client.getCreditDebit());
            }

            if (faultyAccountNumberRecord.getCurrencycode() == null) {
                String currencyId = currencyDao.retrieveCurrencyDefault(client
                        .getProfile());
                Currency currency = currencyDao
                        .retrieveCurrencyOnId(new Integer(currencyId));
                faultyAccountNumberRecord.setCurrencycode(currency.getCode()
                        .toString());
            }

            if (faultyAccountNumberRecord.getCurrencycode().equals(
                    Constants.FL_CURRENCY_CODE)
                    || faultyAccountNumberRecord.getCurrencycode().equals(
                    Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK)) {

                if (faultyAccountNumberRecord.getSign().equalsIgnoreCase(
                        Constants.DEBIT)) {
                    faultyAccRecordAmountDebitFL = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountDebitFL);
                } else {
                    faultyAccRecordAmountCreditFL = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountCreditFL);
                }
            }
            if (faultyAccountNumberRecord.getCurrencycode().equals(
                    Constants.USD_CURRENCY_CODE)) {
                if (faultyAccountNumberRecord.getSign().equalsIgnoreCase(
                        Constants.DEBIT)) {
                    faultyAccRecordAmountDebitUSD = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountDebitUSD);
                } else {
                    faultyAccRecordAmountCreditUSD = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountCreditUSD);
                }
            }
            if (faultyAccountNumberRecord.getCurrencycode().equals(
                    Constants.EUR_CURRENCY_CODE)) {
                if (faultyAccountNumberRecord.getSign().equalsIgnoreCase(
                        Constants.DEBIT)) {
                    faultyAccRecordAmountDebitEUR = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountDebitEUR);
                } else {
                    faultyAccRecordAmountCreditEUR = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountCreditEUR);
                }
            }

            retrievedAmountsFaultyAccountNumber.put("FaultyAccDebitFL",
                    faultyAccRecordAmountDebitFL);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccDebitUSD",
                    faultyAccRecordAmountDebitUSD);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccDebitEUR",
                    faultyAccRecordAmountDebitEUR);

            retrievedAmountsFaultyAccountNumber.put("FaultyAccCreditFL",
                    faultyAccRecordAmountCreditFL);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccCreditUSD",
                    faultyAccRecordAmountCreditUSD);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccCreditEUR",
                    faultyAccRecordAmountCreditEUR);

        }
        return retrievedAmountsFaultyAccountNumber;
    }

    private Map<String, BigDecimal> calculateRetrieveAmounts(
            List<Record> records,
            List<FaultRecord> faultyRecords,
            Client client, FileExtension extension,
            List<TempRecord> faultyAccountNumberRecordList,
            List<TempRecord> sansDuplicateFaultRecordsList) {

        Map<String, BigDecimal> retrievedAmounts = new HashMap<>();

        BigDecimal recordAmountFL = BigDecimal.ZERO;
        BigDecimal recordAmountUSD = BigDecimal.ZERO;
        BigDecimal recordAmountEUR = BigDecimal.ZERO;

        BigDecimal recordAmountDebitFL = BigDecimal.ZERO;
        BigDecimal recordAmountDebitEUR = BigDecimal.ZERO;
        BigDecimal recordAmountDebitUSD = BigDecimal.ZERO;

        BigDecimal recordAmountCreditFL = BigDecimal.ZERO;
        BigDecimal recordAmountCreditEUR = BigDecimal.ZERO;
        BigDecimal recordAmountCreditUSD = BigDecimal.ZERO;

        BigDecimal amountSansDebitFL = BigDecimal.ZERO;
        BigDecimal amountSansDebitUSD = BigDecimal.ZERO;
        BigDecimal amountSansDebitEUR = BigDecimal.ZERO;

        BigDecimal amountSansCreditFL = BigDecimal.ZERO;
        BigDecimal amountSansCreditUSD = BigDecimal.ZERO;
        BigDecimal amountSansCreditEUR = BigDecimal.ZERO;

        BigDecimal totalDebitFL = BigDecimal.ZERO;
        BigDecimal totalDebitUSD = BigDecimal.ZERO;
        BigDecimal totalDebitEUR = BigDecimal.ZERO;

        BigDecimal totalCreditFL = BigDecimal.ZERO;
        BigDecimal totalCreditUSD = BigDecimal.ZERO;
        BigDecimal totalCreditEUR = BigDecimal.ZERO;

        if (client.isBalanced()) {
            for (Record record : records) {
                if (record.getFeeRecord() != 1 && isDebitRecord(record)) {
                    addAmountToTotal(retrievedAmounts, record);
                }
            }
        }
        // Not Balanced
        else {
            for (Record record : records) {
                logger.debug("COUNTERTRANSFER ["+record.getIsCounterTransferRecord()+"] FEERECORD ["+record.getFeeRecord()+"]");

                if (record.isCounterTransferRecord() && !record.hasFee()) {
                    if ((hasFlCurrency(record))) {
                        if (isDebitRecord(record)) {
                            recordAmountDebitFL = record.getAmount().add(recordAmountDebitFL);
                        }
                        if (isCreditRecord(record)) {
                            recordAmountCreditFL = record.getAmount().add(recordAmountCreditFL);
                        }
                    }
                    if (hasEurCurrency(record)) {
                        if (isDebitRecord(record)) {
                            recordAmountDebitEUR = record.getAmount().add(recordAmountDebitEUR);
                        }
                        if (isCreditRecord(record)) {
                            recordAmountCreditEUR = record.getAmount().add(recordAmountCreditEUR);
                        }
                    }
                    if (hasUsdCurrency(record)) {
                        if (isDebitRecord(record)) {
                            recordAmountDebitUSD = record.getAmount().add(recordAmountDebitUSD);
                        }
                        if (isCreditRecord(record)) {
                            recordAmountCreditUSD = record.getAmount().add(recordAmountCreditUSD);
                        }
                    }
                }
            }
            // Sansduplicate
            for (TempRecord sansDupRec : sansDuplicateFaultRecordsList) {
                String currencyCode = sansDupRec.getCurrencycode();
                if (sansDupRec.getSign() == null) {
                    String sign = client.getCreditDebit();
                    sansDupRec.setSign(sign);
                }
                if (currencyCode == null) {
                    String currencyId = currencyDao
                            .retrieveCurrencyDefault(client.getProfile());
                    Currency currency = currencyDao
                            .retrieveCurrencyOnId(new Integer(currencyId));
                    sansDupRec.setCurrencycode(currency.getCode().toString());
                } else {
                    if (currencyCode.equals(Constants.FL_CURRENCY_CODE)
                            || currencyCode.equals(Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK)) {
                        if (sansDupRec.getSign().equalsIgnoreCase(
                                Constants.DEBIT)) {
                            amountSansDebitFL = new BigDecimal(sansDupRec.getAmount()).add(amountSansDebitFL);
                        } else {
                            amountSansCreditFL = new BigDecimal(sansDupRec.getAmount()).add(amountSansCreditFL);
                        }
                    }
                    if (currencyCode.equals(Constants.USD_CURRENCY_CODE)) {
                        if (sansDupRec.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                            amountSansDebitUSD = new BigDecimal(sansDupRec.getAmount()).add(amountSansDebitUSD);
                        } else {
                            amountSansCreditUSD = new BigDecimal(sansDupRec.getAmount()).add(amountSansCreditUSD);
                        }
                    }
                    if (currencyCode.equals(Constants.EUR_CURRENCY_CODE)) {
                        if (sansDupRec.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                            amountSansDebitEUR = new BigDecimal(sansDupRec.getAmount()).add(amountSansDebitEUR);
                        } else {
                            amountSansCreditEUR = new BigDecimal(sansDupRec.getAmount()).add(amountSansCreditEUR);
                        }
                    }
                }
            }

            Map<String, BigDecimal> retrievedAccountNumberAmounts = calculateAmountsFaultyAccountNumber(
                    faultyAccountNumberRecordList, client);
            if (retrievedAccountNumberAmounts.get("FaultyAccDebitFL") != null
                    && amountSansDebitFL != null) {
                totalDebitFL = recordAmountDebitFL.add(amountSansDebitFL)
                        .subtract(retrievedAccountNumberAmounts.get("FaultyAccDebitFL"));
            } else if (amountSansDebitFL != null) {
                totalDebitFL = recordAmountDebitFL.add(amountSansDebitFL);
            } else {
                totalDebitFL = recordAmountDebitFL;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditFL") != null
                    && amountSansCreditFL != null) {
                totalCreditFL = recordAmountCreditFL.add(amountSansCreditFL)
                        .subtract(retrievedAccountNumberAmounts.get("FaultyAccCreditFL"));
            } else if (amountSansCreditFL != null) {
                totalCreditFL = recordAmountCreditFL.add(amountSansCreditFL);
            } else {
                totalCreditFL = recordAmountCreditFL;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccDebitUSD") != null
                    && amountSansDebitUSD != null) {
                totalDebitUSD = recordAmountDebitUSD.add(amountSansDebitUSD)
                        .subtract(retrievedAccountNumberAmounts.get("FaultyAccDebitUSD"));
            } else if (amountSansDebitUSD != null) {
                totalDebitUSD = recordAmountDebitUSD.add(amountSansDebitUSD);
            } else {
                totalDebitUSD = recordAmountDebitUSD;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditUSD") != null
                    && amountSansCreditUSD != null) {
                totalCreditUSD = recordAmountCreditUSD.add(amountSansCreditUSD)
                        .subtract(retrievedAccountNumberAmounts.get("FaultyAccCreditUSD"));
            } else if (amountSansCreditUSD != null) {
                totalCreditUSD = recordAmountCreditUSD.add(amountSansCreditUSD);
            } else {
                totalCreditUSD = recordAmountCreditUSD;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccDebitEUR") != null
                    && amountSansDebitEUR != null) {
                totalDebitEUR = recordAmountDebitEUR.add(amountSansDebitEUR)
                        .subtract(retrievedAccountNumberAmounts.get("FaultyAccDebitEUR"));
            } else if (amountSansDebitEUR != null) {
                totalDebitEUR = recordAmountDebitEUR.add(amountSansDebitEUR);
            } else {
                totalDebitEUR = recordAmountDebitEUR;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditEUR") != null
                    && amountSansCreditEUR != null) {
                totalCreditEUR = recordAmountCreditEUR.add(amountSansCreditEUR)
                        .subtract(retrievedAccountNumberAmounts.get("FaultyAccCreditEUR"));
            } else if (amountSansCreditEUR != null) {
                totalCreditEUR = recordAmountCreditEUR.add(amountSansCreditEUR);
            } else {
                totalCreditEUR = recordAmountCreditEUR;
            }

            recordAmountFL = totalDebitFL.subtract(totalCreditFL).abs();
            recordAmountUSD = totalDebitUSD.subtract(totalCreditUSD).abs();
            recordAmountEUR = totalDebitEUR.subtract(totalCreditEUR).abs();

            retrievedAmounts.put(Constants.CURRENCY_EURO, recordAmountEUR);
            retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountUSD);
            retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountFL);

        }
        return retrievedAmounts;
    }

    private void addAmountToTotal(Map<String, BigDecimal> retrievedAmounts, Record record) {
        String currencyCode = getCurrencyByCode(record.getCurrency().getCode());
        BigDecimal previousValue = retrievedAmounts.get(currencyCode);
        if (previousValue == null) {
            previousValue = BigDecimal.ZERO;
        }
        BigDecimal newValue = previousValue.add(record.getAmount());
        retrievedAmounts.put(currencyCode, newValue);
    }

    protected String getCurrencyByCode(String code) {
        if (Constants.USD_CURRENCY_CODE.equals(code)) {
            return Constants.CURRENCY_USD;
        } else if (Constants.EUR_CURRENCY_CODE.equals(code)) {
            return Constants.CURRENCY_EURO;
        } else if (Constants.FL_CURRENCY_CODE.equals(code)
                || Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK.equals(code)) {
            return Constants.CURRENCY_FL;
        } else {
            throw new IllegalArgumentException("Unknown currency code encountered");
        }
    }

    private boolean isCreditRecord(Record record) {
        return Constants.CREDIT.equalsIgnoreCase(record.getSign());
    }

    private boolean isDebitRecord(Record record) {
        return Constants.DEBIT.equalsIgnoreCase(record.getSign());
    }

    private boolean hasUsdCurrency(Record record) {
        String currencyCode = record.getCurrency().getCode();
        return Constants.USD_CURRENCY_CODE.equals(currencyCode);
    }

    private boolean hasEurCurrency(Record record) {
        String currencyCode = record.getCurrency().getCode();
        return Constants.EUR_CURRENCY_CODE.equals(currencyCode);
    }

    private boolean hasFlCurrency(Record record) {
        String currencyCode = record.getCurrency().getCode();
        return Constants.FL_CURRENCY_CODE.equals(currencyCode)
                || Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK.equals(currencyCode);
    }

    private List<AmountAndRecordsPerBank> amountAndRecords(
            List<Record> records, String transactionType) {
        List<AmountAndRecordsPerBank> list = new ArrayList<AmountAndRecordsPerBank>();
        String typeOfTransaction = transactionType.equalsIgnoreCase(crediting) ? crediting
                : debiting;
        type = typeOfTransaction.equalsIgnoreCase(crediting) ? credit : debit;
        if (transactionType.equalsIgnoreCase(typeOfTransaction)) {
            for (Record record : records) {
                getAmountAndRecords(record, list, transactionType);
            }
        }
        return list;
    }

    private List<AmountAndRecordsPerBank> getAmountAndRecords(Record record,
                                                              List<AmountAndRecordsPerBank> list, String transactionType) {
        Map<String, String> map = new HashMap<String, String>();
        if (record.getFeeRecord().compareTo(0) == 0
                && !map.containsKey(record.getBeneficiaryName())) {

            if (transactionType.equalsIgnoreCase(Constants.CREDITING)) {

                if (record.getBeneficiaryName() != null
                        && !record.getBeneficiaryName().equalsIgnoreCase(
                        Constants.RBTT_BANK_ALTERNATE)) {
                    Boolean newList = true;
                    if (list.size() == 0
                            && record.getSign().equalsIgnoreCase(type)) {
                        // logger.info("bank gegevens: "+record.getSign()+" : "+record.getBank().getName()+" : "+record.getBeneficiaryName());
                        AmountAndRecordsPerBank aARPB = new AmountAndRecordsPerBank();
                        aARPB.setBankName(record.getBank().getName());
                        aARPB.setTotalRecord(1);
                        aARPB.setAmount(record.getAmount());
                        aARPB.setCurrencyType(record.getCurrency()
                                .getCurrencyType());
                        aARPB.setAccountNumber(record
                                .getBeneficiaryAccountNumber());
                        list.add(aARPB);
                        newList = false;
                    }
                    if (newList && record.getSign().equalsIgnoreCase(type)) {
                        // logger.info("bank gegevens: "+record.getSign()+" : "+record.getBank().getName()+" : "+record.getBeneficiaryName());
                        Boolean newRecord = true;
                        for (AmountAndRecordsPerBank object : list) {
                            if (object.getBankName().equalsIgnoreCase(
                                    record.getBank().getName())
                                    && object.getCurrencyType()
                                    .equalsIgnoreCase(
                                            record.getCurrency()
                                                    .getCurrencyType())) {
                                object.setAmount(object.getAmount().add(
                                        record.getAmount()));
                                object
                                        .setTotalRecord(object.getTotalRecord() + 1);
                                newRecord = false;
                            }
                        }
                        if (newRecord) {
                            AmountAndRecordsPerBank aARPB = new AmountAndRecordsPerBank();
                            aARPB.setBankName(record.getBank().getName());
                            aARPB.setTotalRecord(1);
                            aARPB.setAmount(record.getAmount());
                            aARPB.setCurrencyType(record.getCurrency()
                                    .getCurrencyType());
                            aARPB.setAccountNumber(record
                                    .getBeneficiaryAccountNumber());
                            list.add(aARPB);
                        }
                    }
                }
            }

            // del begin
            if (transactionType.equalsIgnoreCase(Constants.DEBITING)) {

                if (record.getBeneficiaryName() == null) {
                    Boolean newList = true;
                    if (list.size() == 0
                            && record.getSign().equalsIgnoreCase(type)) {
                        // logger.info("bank gegevens: "+record.getSign()+" : "+record.getBank().getName()+" : "+record.getBeneficiaryName());
                        AmountAndRecordsPerBank aARPB = new AmountAndRecordsPerBank();
                        aARPB.setBankName(record.getBank().getName());
                        aARPB.setTotalRecord(1);
                        aARPB.setAmount(record.getAmount());
                        aARPB.setCurrencyType(record.getCurrency()
                                .getCurrencyType());
                        aARPB.setAccountNumber(record
                                .getBeneficiaryAccountNumber());
                        list.add(aARPB);
                        newList = false;
                    }
                    if (newList && record.getSign().equalsIgnoreCase(type)) {
                        // logger.info("bank gegevens: "+record.getSign()+" : "+record.getBank().getName()+" : "+record.getBeneficiaryName());
                        Boolean newRecord = true;
                        for (AmountAndRecordsPerBank object : list) {
                            if (object.getBankName().equalsIgnoreCase(
                                    record.getBank().getName())
                                    && object.getCurrencyType()
                                    .equalsIgnoreCase(
                                            record.getCurrency()
                                                    .getCurrencyType())) {
                                object.setAmount(object.getAmount().add(
                                        record.getAmount()));
                                object
                                        .setTotalRecord(object.getTotalRecord() + 1);
                                newRecord = false;
                            }
                        }
                        if (newRecord) {
                            AmountAndRecordsPerBank aARPB = new AmountAndRecordsPerBank();
                            aARPB.setBankName(record.getBank().getName());
                            aARPB.setTotalRecord(1);
                            aARPB.setAmount(record.getAmount());
                            aARPB.setCurrencyType(record.getCurrency()
                                    .getCurrencyType());
                            aARPB.setAccountNumber(record
                                    .getBeneficiaryAccountNumber());
                            list.add(aARPB);
                        }
                    }
                }
            }
            // del end
        }
        return list;
    }

    /*
     *
     * Getters and setters
     */

    public void setCrediting(String crediting) {
        this.crediting = crediting;
    }

    public void setDebiting(String debiting) {
        this.debiting = debiting;
    }

    public void setDebit(String debit) {
        this.debit = debit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public void setLetterSelector(LetterSelector letterSelector) {
        this.letterSelector = letterSelector;
    }

}