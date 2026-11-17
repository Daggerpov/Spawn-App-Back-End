package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.shared.exceptions.GlobalExceptionHandler;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {

    @RestController
    static class ParamController {
        @GetMapping("/params")
        String params(@RequestParam("idToken") String idToken, @RequestParam("provider") OAuthProvider provider) {
            return "ok";
        }

        @PostMapping("/body")
        String body(@RequestBody Map<String, Object> body) {
            return "ok";
        }

        @GetMapping("/boom")
        String boom() {
            throw new RuntimeException("boom");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "logger", mock(ILogger.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new ParamController())
                .setControllerAdvice(handler)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    @Test
    void missingParameter_Returns400NamingTheParameter() throws Exception {
        mockMvc.perform(get("/params").param("provider", "google"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing required parameter: idToken"));
    }

    @Test
    void unknownEnumValue_Returns400NamingTheParameter() throws Exception {
        mockMvc.perform(get("/params").param("idToken", "t").param("provider", "myspace"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: provider"));
    }

    @Test
    void malformedBody_Returns400() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void unexpectedException_StillReturns500() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }
}
