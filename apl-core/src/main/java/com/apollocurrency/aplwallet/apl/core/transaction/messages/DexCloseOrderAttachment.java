package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
public class DexCloseOrderAttachment extends AbstractAttachment {

    private long contractId;

    public DexCloseOrderAttachment(ByteBuffer buffer) {
        super(buffer);
        this.contractId = buffer.getLong();
    }

    public DexCloseOrderAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.contractId = Convert.parseUnsignedLong((String) attachmentData.get("contractId"));
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.contractId);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("contractId", Long.toUnsignedString(this.getContractId()));
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CLOSE_ORDER;
    }
}