package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JsonContractTest {
    @Test void fractionalCoordinatesAreRejectedInsteadOfTruncated() {
        var builder = JsonMapper.builder();
        new JsonConfiguration().strictNumbers().customize(builder);
        var mapper = builder.build();
        assertThrows(Exception.class, () -> mapper.readValue("{\"requestId\":\"58260481-7e3b-41b4-aa77-0d57a4f57382\",\"expectedVersion\":0,\"x\":1.5,\"y\":2}", SceneController.MoveRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"requestId\":\"58260481-7e3b-41b4-aa77-0d57a4f57382\",\"expectedVersion\":0.5,\"x\":1,\"y\":2}", SceneController.MoveRequest.class));
    }
    @Test void stringCoordinatesAndVersionsAreRejected() {
        var builder = JsonMapper.builder();
        new JsonConfiguration().strictNumbers().customize(builder);
        var mapper = builder.build();
        assertThrows(Exception.class, () -> mapper.readValue("{\"requestId\":\"58260481-7e3b-41b4-aa77-0d57a4f57382\",\"expectedVersion\":0,\"x\":\"1\",\"y\":2}", SceneController.MoveRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"requestId\":\"58260481-7e3b-41b4-aa77-0d57a4f57382\",\"expectedVersion\":\"0\",\"x\":1,\"y\":2}", SceneController.MoveRequest.class));
    }
}
