package com.evbox.everon.ocpp.it.provisioning;

import com.evbox.everon.ocpp.mock.StationSimulatorSetUp;
import com.evbox.everon.ocpp.mock.ocpp.exchange.NotifyReport;
import com.evbox.everon.ocpp.simulator.message.ActionType;
import com.evbox.everon.ocpp.simulator.message.Call;
import com.evbox.everon.ocpp.v20.message.station.GetBaseReportRequest;
import com.evbox.everon.ocpp.v20.message.station.GetBaseReportResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.evbox.everon.ocpp.mock.constants.StationConstants.STATION_ID;
import static com.evbox.everon.ocpp.v20.message.station.GetBaseReportRequest.ReportBase.CONFIGURATION_INVENTORY;
import static com.evbox.everon.ocpp.v20.message.station.GetBaseReportRequest.ReportBase.FULL_INVENTORY;
import static com.evbox.everon.ocpp.v20.message.station.GetBaseReportResponse.Status.ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GetBaseReportIt extends StationSimulatorSetUp {

    private static int NOTIFY_REPORT_VARIABLES_LESS_ONE = 8;

    @Test
    void shouldReplyToGetBaseReportRequest() {

        String id = UUID.randomUUID().toString();

        stationSimulatorRunner.run();

        GetBaseReportRequest request = new GetBaseReportRequest().withReportBase(CONFIGURATION_INVENTORY);
        Call call = new Call(id, ActionType.GET_BASE_REPORT, request);

        ocppMockServer.waitUntilConnected();

        GetBaseReportResponse response =
                ocppServerClient.findStationSender(STATION_ID).sendMessage(call.toJson(), GetBaseReportResponse.class);

        await().untilAsserted(() -> {
            assertThat(response.getStatus()).isEqualTo(ACCEPTED);
            ocppMockServer.verify();
        });
    }

    @Test
    void shouldSendNotifyReportOnGetBaseReportRequest() {

        for (int i = 0; i < NOTIFY_REPORT_VARIABLES_LESS_ONE; i++) {
            ocppMockServer
                    .when(NotifyReport.request(i, true))
                    .thenReturn(NotifyReport.response());
        }

        ocppMockServer
                .when(NotifyReport.request(NOTIFY_REPORT_VARIABLES_LESS_ONE, false))
                .thenReturn(NotifyReport.response());

        stationSimulatorRunner.run();

        GetBaseReportRequest request = new GetBaseReportRequest().withReportBase(FULL_INVENTORY);
        Call call = new Call(UUID.randomUUID().toString(), ActionType.GET_BASE_REPORT, request);

        ocppMockServer.waitUntilConnected();

        ocppServerClient.findStationSender(STATION_ID).sendMessage(call.toJson());

        await().untilAsserted(() -> {
            ocppMockServer.verify();
        });
    }
}
