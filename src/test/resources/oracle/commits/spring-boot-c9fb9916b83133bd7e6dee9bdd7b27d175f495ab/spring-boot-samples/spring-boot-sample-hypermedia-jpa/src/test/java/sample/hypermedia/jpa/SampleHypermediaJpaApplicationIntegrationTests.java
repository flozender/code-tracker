/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.hypermedia.jpa;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SampleHypermediaJpaApplication.class)
@WebAppConfiguration
@DirtiesContext
public class SampleHypermediaJpaApplicationIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void links() throws Exception {
		this.mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void health() throws Exception {
		this.mockMvc.perform(get("/admin/health").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void adminLinks() throws Exception {
		this.mockMvc.perform(get("/admin").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void docs() throws Exception {
		MvcResult response = this.mockMvc
				.perform(get("/admin/docs/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		System.err.println(response.getResponse().getContentAsString());
	}

	@Test
	public void browser() throws Exception {
		MvcResult response = this.mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isFound()).andReturn();
		assertEquals("/browser/index.html#",
				response.getResponse().getHeaders("location").get(0));
	}

}
