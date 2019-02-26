package com.evbox.everon.ocpp.simulator.support;

import com.evbox.everon.ocpp.simulator.station.evse.*;

import java.util.Collections;

public class EvseCreator {

    public static final Evse DEFAULT_EVSE_INSTANCE = createEvse()
            .withId(StationConstants.DEFAULT_EVSE_ID)
            .withStatus(EvseStatus.AVAILABLE)
            .withConnectorIdAndCableStatus(StationConstants.DEFAULT_CONNECTOR_ID, CableStatus.UNPLUGGED)
            .withTransaction(new EvseTransaction(StationConstants.DEFAULT_INT_TRANSACTION_ID))
            .build();

    public static EvseBuilder createEvse() {
        return new EvseBuilder();
    }

    public static class EvseBuilder {

        private int id;
        private EvseStatus evseStatus;
        private int connectorId;
        private CableStatus cableStatus;
        private EvseTransaction evseTransaction;

        public EvseBuilder withId(int id) {
            this.id = id;
            return this;
        }

        public EvseBuilder withStatus(EvseStatus evseStatus) {
            this.evseStatus = evseStatus;
            return this;
        }

        public EvseBuilder withConnectorIdAndCableStatus(int connectorId, CableStatus cableStatus) {
            this.connectorId = connectorId;
            this.cableStatus = cableStatus;
            return this;
        }

        public EvseBuilder withTransaction(EvseTransaction evseTransaction) {
            this.evseTransaction = evseTransaction;
            return this;
        }

        public Evse build() {
            return new Evse(id, evseStatus, evseTransaction, Collections.singletonList(new Connector(connectorId, cableStatus)));
        }

    }
}
