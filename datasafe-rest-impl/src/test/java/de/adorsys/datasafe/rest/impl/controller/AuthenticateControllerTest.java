package de.adorsys.datasafe.rest.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.datasafe.business.impl.service.DefaultDatasafeServices;
import de.adorsys.datasafe.inbox.impl.InboxServiceImpl;
import de.adorsys.datasafe.rest.impl.dto.UserDTO;
import de.adorsys.datasafe.rest.impl.security.SecurityConstants;
import de.adorsys.datasafe.rest.impl.security.SecurityProperties;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.experimental.results.ResultMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.StatusResultMatchers;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthenticateControllerTest extends BaseDatasafeEndpointTest {

    private static final String DEFAULT_TEST_USERNAME = "username";
    private static final String DEFAULT_TEST_PASSWORD = "password";

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static final String TEST_USER = "test";
    private static final String TEST_PATH = "test.txt";

    @MockBean
    private DefaultDatasafeServices dataSafeService;

    @MockBean
    private InboxServiceImpl inboxService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(dataSafeService.inboxService()).thenReturn(inboxService);
    }

    @Test
    void testAuthenticateSuccess() {
        MockitoAnnotations.initMocks(this);

        UserDTO userDTO = new UserDTO();
        userDTO.setUserName("username");
        userDTO.setPassword("password");

        String authorizationToken = sendAuthenticateRequest(userDTO).getResponse()
                .getHeader(SecurityConstants.TOKEN_HEADER);

        assertTrue(StringUtils.isNotBlank(authorizationToken));
        assertTrue(authorizationToken.startsWith(SecurityConstants.TOKEN_PREFIX));
    }

    @Test
    @SneakyThrows
    void testAuthenticateFailWithIncorrectCredentials() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserName("test");
        userDTO.setPassword("test");

        String response = sendAuthenticateRequestWithStatus(userDTO, status().isForbidden())
                .getResponse()
                .getErrorMessage();

        assertTrue(StringUtils.isNotBlank(response));
        assertEquals("Access Denied", response);
    }

    @SneakyThrows
    @Test
    void testGetDataWithToken() {
        when(dataSafeService.inboxService().write(any())).thenReturn(new ByteArrayOutputStream());
        UserDTO userDTO = new UserDTO();
        userDTO.setUserName(DEFAULT_TEST_USERNAME);
        userDTO.setPassword(DEFAULT_TEST_PASSWORD);

        String token = sendAuthenticateRequest(userDTO).getResponse().getHeader(SecurityConstants.TOKEN_HEADER);

        mvc.perform(
               put("/inbox/{path}", TEST_PATH).
               contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE).
               header("user", TEST_USER).
               header(SecurityConstants.TOKEN_HEADER, token))
           .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void testGetDataWithoutTokenFail() {
        when(dataSafeService.inboxService().write(any())).thenReturn(new ByteArrayOutputStream());

        String errorMessage = mvc
                    .perform(
                        put("/inbox/{path}", TEST_PATH).
                        contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE).
                        header("user", TEST_USER))
                    .andExpect(status().isForbidden())
                    .andReturn()
                    .getResponse()
                    .getErrorMessage();

        assertEquals("Access Denied", errorMessage);

    }

    @SneakyThrows
    private MvcResult sendAuthenticateRequest(UserDTO userDTO) {
        return sendAuthenticateRequestWithStatus(userDTO, status().isOk());
    }

    @SneakyThrows
    private MvcResult sendAuthenticateRequestWithStatus(UserDTO userDTO, ResultMatcher statusMatcher) {
        return this.mvc
                .perform(post("/api/authenticate").
                        content(jsonMapper.writeValueAsString(userDTO)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(statusMatcher)
                .andReturn();
    }
}
