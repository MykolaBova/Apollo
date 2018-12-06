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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.addons;


import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public final class AfterStart implements AddOn {
    private static final Logger LOG = getLogger(AfterStart.class);
    // TODO: YL remove static instance later
    private static AplGlobalObjects aplGlobalObjects = CDI.current().select(AplGlobalObjects.class).get();

    @Override
    public void init() {
        String afterStartScript = aplGlobalObjects.getStringProperty("apl.afterStartScript");
        if (afterStartScript != null) {
            ThreadPool.runAfterStart("After start script", () -> {
                try {
                    Runtime.getRuntime().exec(afterStartScript);
                } catch (Exception e) {
                    LOG.error("Failed to run after start script: " + afterStartScript, e);
                }
            });
        }
    }

    @Override
    public void processRequest(Map<String, String> map) {
        LOG.info(map.get("afterStartMessage"));
    }
}
