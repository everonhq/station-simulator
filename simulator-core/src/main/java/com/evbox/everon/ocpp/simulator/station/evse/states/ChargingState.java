package com.evbox.everon.ocpp.simulator.station.evse.states;

import com.evbox.everon.ocpp.common.OptionList;
import com.evbox.everon.ocpp.simulator.station.StationMessageSender;
import com.evbox.everon.ocpp.simulator.station.StationStore;
import com.evbox.everon.ocpp.simulator.station.actions.user.UserMessageResult;
import com.evbox.everon.ocpp.simulator.station.component.transactionctrlr.TxStartStopPointVariableValues;
import com.evbox.everon.ocpp.simulator.station.evse.Connector;
import com.evbox.everon.ocpp.simulator.station.evse.Evse;
import com.evbox.everon.ocpp.simulator.station.evse.states.helpers.AuthorizeHelper;
import com.evbox.everon.ocpp.v20.message.station.AuthorizeResponse;
import com.evbox.everon.ocpp.v20.message.station.IdTokenInfo;
import com.evbox.everon.ocpp.v20.message.station.TransactionData;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static com.evbox.everon.ocpp.v20.message.station.TransactionEventRequest.TriggerReason.REMOTE_STOP;
import static com.evbox.everon.ocpp.v20.message.station.TransactionEventRequest.TriggerReason.STOP_AUTHORIZED;
import static java.util.Collections.singletonList;

/**
 *  When the transaction is ongoing and the evse is charging.
 */
@Slf4j
public class ChargingState extends AbstractEvseState {

    public static final String NAME =  "CHARGING";

    @Override
    public String getStateName() {
        return NAME;
    }

    @Override
    public CompletableFuture<UserMessageResult> onPlug(int evseId, int connectorId) {
        return CompletableFuture.completedFuture(UserMessageResult.NOT_EXECUTED);
    }

    @Override
    public CompletableFuture<UserMessageResult> onAuthorize(int evseId, String tokenId) {
        log.info("in authorizeToken {}", tokenId);

        if (!isTokenThatStarted(evseId, tokenId)) {
            log.error("Cannot stop transaction with different token than start one.");
            return CompletableFuture.completedFuture(UserMessageResult.FAILED);
        }

        CompletableFuture<UserMessageResult> future = new CompletableFuture<>();
        StationMessageSender stationMessageSender = stateManager.getStationMessageSender();
        stationMessageSender.sendAuthorizeAndSubscribe(tokenId, singletonList(evseId),
                (request, response) -> handleAuthorizeResponse(evseId, tokenId, response, future));

        return future;
    }

    private boolean isTokenThatStarted(int evseId, String tokenId) {
        StationStore stationStore = stateManager.getStationStore();
        Evse evse = stationStore.findEvse(evseId);
        return evse.getTokenId().equals(tokenId);
    }

    private void handleAuthorizeResponse(int evseId, String tokenId, AuthorizeResponse response, CompletableFuture<UserMessageResult> future) {
        StationMessageSender stationMessageSender = stateManager.getStationMessageSender();
        StationStore stationStore = stateManager.getStationStore();
        Evse evse = stationStore.findEvse(evseId);
        OptionList<TxStartStopPointVariableValues> stopPoints = stationStore.getTxStopPointValues();

        if (response.getIdTokenInfo().getStatus() == IdTokenInfo.Status.ACCEPTED) {
            evse.setToken(tokenId);
            int connectorId = stopCharging(stationMessageSender, evse);

            if (stopPoints.contains(TxStartStopPointVariableValues.AUTHORIZED) && !stopPoints.contains(TxStartStopPointVariableValues.POWER_PATH_CLOSED)) {
                evse.stopTransaction();
                evse.clearToken();

                stationMessageSender.sendTransactionEventEnded(evseId, connectorId, STOP_AUTHORIZED, TransactionData.StoppedReason.DE_AUTHORIZED, evse.getWattConsumedLastSession());
            }
            stateManager.setStateForEvse(evseId, new StoppedState());
            future.complete(UserMessageResult.SUCCESSFUL);
        } else  {
            AuthorizeHelper.handleFailedAuthorizeResponse(stateManager, evse);

            if (evse.hasOngoingTransaction()) {
                if (!stopPoints.contains(TxStartStopPointVariableValues.AUTHORIZED)) {
                    stateManager.setStateForEvse(evseId, new WaitingForAuthorizationState());
                } else {
                    stateManager.setStateForEvse(evseId, new StoppedState());
                }

                evse.tryUnlockConnector();
            }
            future.complete(UserMessageResult.FAILED);
        }
    }

    @Override
    public CompletableFuture<UserMessageResult> onUnplug(int evseId, int connectorId) {
        return CompletableFuture.completedFuture(UserMessageResult.NOT_EXECUTED);
    }

    @Override
    public void onRemoteStart(int evseId, int remoteStartId, String tokenId, Connector connector) {
        // NOP
    }

    @Override
    public void onRemoteStop(int evseId) {
        Evse evse = stateManager.getStationStore().findEvse(evseId);
        StationMessageSender stationMessageSender = stateManager.getStationMessageSender();

        evse.stopCharging();
        Integer connectorId = evse.tryUnlockConnector();
        stationMessageSender.sendTransactionEventUpdate(evseId, connectorId, REMOTE_STOP, TransactionData.ChargingState.EV_DETECTED);

        stateManager.setStateForEvse(evseId, new RemotelyStoppedState());
    }

    private int stopCharging(StationMessageSender stationMessageSender, Evse evse) {
        evse.stopCharging();
        Integer connectorId = evse.unlockConnector();
        stationMessageSender.sendTransactionEventUpdate(evse.getId(), connectorId, STOP_AUTHORIZED, TransactionData.ChargingState.EV_DETECTED);
        return connectorId;
    }

}
