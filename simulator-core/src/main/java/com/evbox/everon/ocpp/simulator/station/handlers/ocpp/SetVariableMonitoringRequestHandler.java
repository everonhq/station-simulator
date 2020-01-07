package com.evbox.everon.ocpp.simulator.station.handlers.ocpp;

import com.evbox.everon.ocpp.common.CiString;
import com.evbox.everon.ocpp.simulator.station.StationMessageSender;
import com.evbox.everon.ocpp.simulator.station.component.StationComponent;
import com.evbox.everon.ocpp.simulator.station.component.StationComponentsHolder;
import com.evbox.everon.ocpp.v20.message.centralserver.SetMonitoringDatum;
import com.evbox.everon.ocpp.v20.message.centralserver.SetMonitoringResult;
import com.evbox.everon.ocpp.v20.message.centralserver.SetVariableMonitoringRequest;
import com.evbox.everon.ocpp.v20.message.centralserver.SetVariableMonitoringResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handler for {@link SetVariableMonitoringRequest} request.
 */
@Slf4j
public class SetVariableMonitoringRequestHandler implements OcppRequestHandler<SetVariableMonitoringRequest> {

    private StationComponentsHolder stationComponentsHolder;
    private StationMessageSender stationMessageSender;

    public SetVariableMonitoringRequestHandler(StationComponentsHolder stationComponentsHolder, StationMessageSender stationMessageSender) {
        this.stationComponentsHolder = stationComponentsHolder;
        this.stationMessageSender = stationMessageSender;
    }

    /**
     * Handle {@link SetVariableMonitoringRequest} request.
     *
     * @param callId identity of the message
     * @param request incoming request from the server
     */
    @Override
    public void handle(String callId, SetVariableMonitoringRequest request) {
        List<SetMonitoringResult> results = new ArrayList<>();

        for (SetMonitoringDatum data : request.getSetMonitoringData()) {
            SetMonitoringResult monitoringResult = buildResponse(data);

            String componentName = data.getComponent().getName().toString();
            Optional<StationComponent> component = stationComponentsHolder.getComponent(new CiString.CiString50(componentName));
            if (!component.isPresent()) {
                results.add(monitoringResult.withStatus(SetMonitoringResult.Status.UNKNOWN_COMPONENT));
                continue;
            }

            String variableName = data.getVariable().getName().toString();
            if (!component.get().getVariableNames().contains(variableName)) {
                results.add(monitoringResult.withStatus(SetMonitoringResult.Status.UNKNOWN_VARIABLE));
            } else {
                int id = Optional.ofNullable(data.getId()).orElseGet(() -> ThreadLocalRandom.current().nextInt());
                stationComponentsHolder.monitorComponent(id, componentName, variableName);
                monitoringResult = monitoringResult.withId(id).withStatus(SetMonitoringResult.Status.ACCEPTED);
                results.add(monitoringResult);
            }
        }

        stationMessageSender.sendCallResult(callId, new SetVariableMonitoringResponse().withSetMonitoringResult(results));
    }

    private SetMonitoringResult buildResponse(SetMonitoringDatum request) {
        return new SetMonitoringResult()
                .withType(SetMonitoringResult.Type.fromValue(request.getType().value()))
                .withSeverity(request.getSeverity())
                .withComponent(request.getComponent())
                .withVariable(request.getVariable());
    }
}