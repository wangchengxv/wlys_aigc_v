package com.miioo.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhoneLoginIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoginByPhoneWhenCodeValid() throws Exception {
        MvcResult sendCodeResult = mockMvc.perform(get("/api/auth/send-code")
                        .param("phone", "13812345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode json = objectMapper.readTree(sendCodeResult.getResponse().getContentAsString());
        String code = json.path("data").path("debugCode").asText();

        mockMvc.perform(post("/api/auth/login/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13812345678","code":"%s","autoLogin":true}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.phone").value("13812345678"));
    }

    @Test
    void shouldRejectWhenCodeInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13812345678","code":"000000","autoLogin":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
