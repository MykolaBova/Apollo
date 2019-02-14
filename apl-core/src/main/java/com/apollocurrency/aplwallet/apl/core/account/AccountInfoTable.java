/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
class AccountInfoTable extends VersionedEntityDbTable<AccountInfo> {

    public AccountInfoTable(String table, DbKey.Factory<AccountInfo> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, fullTextSearchColumns);
    }

    @Override
    protected AccountInfo load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountInfo(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountInfo accountInfo) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_info " + "(account_id, name, description, height, latest) " + "KEY (account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountInfo.accountId);
            DbUtils.setString(pstmt, ++i, accountInfo.name);
            DbUtils.setString(pstmt, ++i, accountInfo.description);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }
    
}
