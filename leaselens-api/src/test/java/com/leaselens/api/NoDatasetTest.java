package com.leaselens.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Before any dataset is loaded, analytics endpoints answer 409 with an explanation. */
@SpringBootTest(properties = "leaselens.autoload-sample=false")
@AutoConfigureMockMvc
class NoDatasetTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Every analytics endpoint returns 409 when no dataset is loaded")
    void conflictWithoutDataset() throws Exception {
        for (String path : new String[] {"/api/datasets/current", "/api/portfolio/overview",
                "/api/properties/performance", "/api/cashflow/range", "/api/payments/percentiles",
                "/api/payments/outstanding", "/api/expenses/summary"}) {
            mvc.perform(get(path))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.title").value("No dataset loaded"));
        }
    }
}
