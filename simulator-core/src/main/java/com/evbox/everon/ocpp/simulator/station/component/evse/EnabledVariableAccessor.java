package com.evbox.everon.ocpp.simulator.station.component.evse;

import com.evbox.everon.ocpp.common.CiString;
import com.evbox.everon.ocpp.simulator.station.Station;
import com.evbox.everon.ocpp.simulator.station.component.variable.SetVariableValidator;
import com.evbox.everon.ocpp.simulator.station.component.variable.VariableAccessor;
import com.evbox.everon.ocpp.simulator.station.component.variable.VariableGetter;
import com.evbox.everon.ocpp.simulator.station.component.variable.VariableSetter;
import com.evbox.everon.ocpp.v20.message.centralserver.*;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

public class EnabledVariableAccessor extends VariableAccessor {

    public static final String NAME = "Enabled";
    public static final String EVSE_ENABLED_STATUS = "true";

    private final Map<GetVariableDatum.AttributeType, VariableGetter> variableGetters = ImmutableMap.<GetVariableDatum.AttributeType, VariableGetter>builder()
            .put(GetVariableDatum.AttributeType.ACTUAL, this::getActualValue)
            .build();

    private final Map<SetVariableDatum.AttributeType, SetVariableValidator> variableValidators = ImmutableMap.<SetVariableDatum.AttributeType, SetVariableValidator>builder()
            .put(SetVariableDatum.AttributeType.ACTUAL, this::rejectVariable)
            .build();

    public EnabledVariableAccessor(Station station) {
        super(station);
    }

    @Override
    public String getVariableName() {
        return NAME;
    }

    @Override
    public Map<GetVariableDatum.AttributeType, VariableGetter> getVariableGetters() {
        return variableGetters;
    }

    @Override
    public Map<SetVariableDatum.AttributeType, VariableSetter> getVariableSetters() {
        return Collections.emptyMap();
    }

    @Override
    public Map<SetVariableDatum.AttributeType, SetVariableValidator> getVariableValidators() {
        return variableValidators;
    }

    private SetVariableResult rejectVariable(Component component, Variable variable, SetVariableDatum.AttributeType attributeType, CiString.CiString1000 ciString1000) {
        return new SetVariableResult()
                .withComponent(component)
                .withVariable(variable)
                .withAttributeType(SetVariableResult.AttributeType.fromValue(attributeType.value()))
                .withAttributeStatus(SetVariableResult.AttributeStatus.REJECTED);
    }

    private GetVariableResult getActualValue(Component component, Variable variable, GetVariableDatum.AttributeType attributeType) {
        Integer evseId = component.getEvse().getId();

        GetVariableResult getVariableResult = new GetVariableResult()
                .withComponent(component)
                .withVariable(variable)
                .withAttributeType(GetVariableResult.AttributeType.fromValue(attributeType.value()));

        boolean evseExists = getStation().getState().hasEvse(evseId);

        if (evseExists) {
            return getVariableResult
                    .withAttributeValue(new CiString.CiString1000(EVSE_ENABLED_STATUS))
                    .withAttributeStatus(GetVariableResult.AttributeStatus.ACCEPTED);
        } else {
            return getVariableResult.withAttributeStatus(GetVariableResult.AttributeStatus.REJECTED);
        }
    }
}