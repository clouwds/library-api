package de.clouwds.library_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CsrfRoundTripTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String JSON_STRING = "{\n\"firstName\": \"MockMvc\",\n\"lastName\": \"Test\",\n\"email\": \"mock@test.com\",\n\"password\": \"0000\",\n\"role\": \"LIBRARIAN\"\n}";


//    @Test
//    public void testNotAuthorizedWithoutCsrf() throws Exception {
//        //simulated http call
//        mockMvc.perform(post("/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(JSON_STRING))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void testSuccessWithCsrf() throws Exception {
//        // .with(csrf()) simulates a client that legitimately obtained a valid token without manually extracting one
//        mockMvc.perform(post("/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(JSON_STRING)
//                .with(csrf()))
//                .andExpect(status().isCreated());
//    }
}
