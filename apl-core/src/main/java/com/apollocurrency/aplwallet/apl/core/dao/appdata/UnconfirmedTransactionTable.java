/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Singleton
public class UnconfirmedTransactionTable extends EntityDbTable<UnconfirmedTransaction> {

    private final LongKeyFactory<UnconfirmedTransaction> transactionKeyFactory;
    private int maxUnconfirmedTransactions;
    private final Map<DbKey, UnconfirmedTransaction> transactionCache = new HashMap<>();
    private final PriorityQueue<UnconfirmedTransaction> waitingTransactions;
    private final Map<TransactionType, Map<String, Integer>> unconfirmedDuplicates = new HashMap<>();

    @Inject
    public UnconfirmedTransactionTable(LongKeyFactory<UnconfirmedTransaction> transactionKeyFactory,
                                       PropertiesHolder propertiesHolder) {
        super("unconfirmed_transaction", transactionKeyFactory);
        this.transactionKeyFactory = transactionKeyFactory;
        int n = propertiesHolder.getIntProperty("apl.maxUnconfirmedTransactions");
        maxUnconfirmedTransactions = n <= 0 ? Integer.MAX_VALUE : n;
        this.waitingTransactions = createWaitingTransactionsQueue();
    }

    @Override
    public UnconfirmedTransaction load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new UnconfirmedTransaction(rs);
    }

    @Override
    public void save(Connection con, UnconfirmedTransaction unconfirmedTransaction) throws SQLException {
//        unconfirmedTransaction.save(con);
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO unconfirmed_transaction (id, transaction_height, "
            + "fee_per_byte, expiration, transaction_bytes, prunable_json, arrival_timestamp, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, unconfirmedTransaction.getId());
            pstmt.setInt(++i, unconfirmedTransaction.getHeight());
            pstmt.setLong(++i, unconfirmedTransaction.getFeePerByte());
            pstmt.setInt(++i, unconfirmedTransaction.getExpiration());
            pstmt.setBytes(++i, unconfirmedTransaction.getBytes());
            JSONObject prunableJSON = unconfirmedTransaction.getPrunableAttachmentJSON();
            if (prunableJSON != null) {
                pstmt.setString(++i, prunableJSON.toJSONString());
            } else {
                pstmt.setNull(++i, Types.VARCHAR);
            }
            pstmt.setLong(++i, unconfirmedTransaction.getArrivalTimestamp());
            pstmt.setInt(++i, unconfirmedTransaction.getHeight());
            pstmt.executeUpdate();
        }

        if (transactionCache.size() < maxUnconfirmedTransactions) {
            DbKey dbKey = transactionKeyFactory.newKey(unconfirmedTransaction.getId());
            transactionCache.put(dbKey, unconfirmedTransaction);
        }
    }

    @Override
    public int rollback(int height) {
        int rc;
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE height > ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UnconfirmedTransaction unconfirmedTransaction = load(con, rs, null);
                    waitingTransactions.add(unconfirmedTransaction);
                    DbKey dbKey = transactionKeyFactory.newKey(unconfirmedTransaction.getId());
                    transactionCache.remove(dbKey);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        rc = super.rollback(height);
        unconfirmedDuplicates.clear();
        return rc;
    }

    @Override
    public void truncate() {
        super.truncate();
    }

    @Override
    public String defaultSort() {
        return " ORDER BY transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC, id ASC ";
    }

    private PriorityQueue<UnconfirmedTransaction> createWaitingTransactionsQueue() {
        return new PriorityQueue<>(
            (o1, o2) -> {
                int result;
                if ((result = Integer.compare(o2.getHeight(), o1.getHeight())) != 0) {
                    return result;
                }
                if ((result = Boolean.compare(o2.getTransaction().referencedTransactionFullHash() != null,
                    o1.getTransaction().referencedTransactionFullHash() != null)) != 0) {
                    return result;
                }
                if ((result = Long.compare(o1.getFeePerByte(), o2.getFeePerByte())) != 0) {
                    return result;
                }
                if ((result = Long.compare(o2.getArrivalTimestamp(), o1.getArrivalTimestamp())) != 0) {
                    return result;
                }
                return Long.compare(o2.getId(), o1.getId());
            }) {

            @Override
            public boolean add(UnconfirmedTransaction unconfirmedTransaction) {
                if (!super.add(unconfirmedTransaction)) {
                    return false;
                }
                if (this.size() > maxUnconfirmedTransactions) {
                    UnconfirmedTransaction removed = remove();
                    //LOG.debug("Dropped unconfirmed transaction " + removed.getJSONObject().toJSONString());
                }
                return true;
            }

        };
    }

}
