/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.transport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Serhiy Lymar
 */
public class TransportStatusReply {
    @JsonProperty("type")
    public String type;
    @JsonProperty("status")
    public String status;
    @JsonProperty("remoteip")
    public String remoteip;
    @JsonProperty("remoteport")
    public int remoteport;
    @JsonProperty("tunaddr")
    public String tunaddr;
    @JsonProperty("tunnetmask")
    public String tunnetmask;
    @JsonProperty("id")
    public int id;
}
