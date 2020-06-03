/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NOT_ENOUGH_ASSETS;

@Vetoed
public final class TransferAsset extends CreateTransaction {

    public TransferAsset() {
        super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, "recipient", "asset", "quantityATU");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long recipient = HttpParameterParserUtil.getAccountId(req, "recipient", true);

        Asset asset = HttpParameterParserUtil.getAsset(req);
        long quantityATU = HttpParameterParserUtil.getQuantityATU(req);
        Account account = HttpParameterParserUtil.getSenderAccount(req);

        Attachment attachment = new ColoredCoinsAssetTransfer(asset.getId(), quantityATU);
        try {
            return createTransaction(req, account, recipient, 0, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return NOT_ENOUGH_ASSETS;
        }
    }

}
