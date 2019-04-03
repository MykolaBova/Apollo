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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetShuffling extends AbstractAPIRequestHandler {

    private static class GetShufflingHolder {
        private static final GetShuffling INSTANCE = new GetShuffling();
    }

    public static GetShuffling getInstance() {
        return GetShufflingHolder.INSTANCE;
    }

    private GetShuffling() {
        super(new APITag[] {APITag.SHUFFLING}, "shuffling", "includeHoldingInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        boolean includeHoldingInfo = "true".equalsIgnoreCase(req.getParameter("includeHoldingInfo"));
        return JSONData.shuffling(ParameterParser.getShuffling(req), includeHoldingInfo);
    }

}
