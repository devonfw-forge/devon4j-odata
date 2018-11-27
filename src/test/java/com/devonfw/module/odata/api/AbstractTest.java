package com.devonfw.module.odata.api;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

import org.dozer.Mapper;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import com.sap.cloud.sdk.testutil.MockUtil;

import static com.devonfw.sample.constant.ServletConstants.ODATA_NAME;
import static com.devonfw.sample.constant.ServletConstants.PATH_TO_SERVICE;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractTest {

    protected static final String HOSTNAME = "localhost";

    protected static final String TEST_ODATA_NAME = ODATA_NAME;

    protected static final String TEST_DESTINATION_NAME = "BUDGETING";

    protected static final String ID_KEY = "Id";

    private static MockUtil mockUtil = new MockUtil();

    @Inject
    protected Mapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @LocalManagementPort
    private int randomManagementPort;

    @Before
    public void beforeEach() throws URISyntaxException {

        try {
            String url = "http://" + HOSTNAME + ":" + randomManagementPort + PATH_TO_SERVICE;
            mockUtil.mockDestination(TEST_DESTINATION_NAME, new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
