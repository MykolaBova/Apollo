package com.apollocurrency.aplwallet.apl.core.phasing.mapper;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingApprovedResult;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingApprovedResultMapper  extends DerivedEntityMapper<PhasingApprovedResult> {

    public PhasingApprovedResultMapper(KeyFactory<PhasingApprovedResult> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PhasingApprovedResult doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long phasingTx = rs.getLong("phasing_tx");
        long approvedTx = rs.getLong("approved_tx");
        return new PhasingApprovedResult(phasingTx, approvedTx);
    }
}