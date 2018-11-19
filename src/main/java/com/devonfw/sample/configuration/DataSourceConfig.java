package com.devonfw.sample.configuration;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * @author manjinsi
 */

@Configuration public class DataSourceConfig {

        public final String VCAP_DB_TYPE = "hana";

        public final String VCAP_DB_CREDENTIALS = "credentials";

        public final String VCAP_DB_USER = "user";

        public final String VCAP_DB_PASSWORD = "password";

        public final String VCAP_DB_URL = "url";

        public final String VCAP_DB_DRIVER = "driver";

        public final String SPRING_USER = "spring.datasource.username";

        public final String SPRING_PASSWORD = "spring.datasource.password";

        public final String SPRING_URL = "spring.datasource.url";

        public final String SPRING_DRIVERCLASS = "spring.datasource.driver-class-name";

        public String username;

        public String password;

        public String url;

        public String driverClassName;

        @Inject private Environment env;

        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

        @Bean @Primary public DataSource dataSource() {

                String VCAP_SERVICES = System.getenv("VCAP_SERVICES");

                if (checkVCAP(VCAP_SERVICES) && VCAP_SERVICES.contains(VCAP_DB_TYPE)) {
                        setDataSourcePropertiesFromVCAP(VCAP_SERVICES);
                } else {
                        setDataSourcePropertiesFromPropertyFile();
                }

                return DataSourceBuilder.create().username(this.username).password(this.password).url(this.url).driverClassName(this.driverClassName).build();
        }

        /**
         * Check VCAP_SERVICES environment variable.
         */
        private Boolean checkVCAP(String VCAP_SERVICES) {

                return !(VCAP_SERVICES == null || VCAP_SERVICES.equals("{}"));
        }

        /**
         * Setting up connection parameters from Spring property file
         */
        public void setDataSourcePropertiesFromPropertyFile() {

                this.username = this.env.getProperty(this.SPRING_USER);
                this.password = this.env.getProperty(this.SPRING_PASSWORD);
                this.url = this.env.getProperty(this.SPRING_URL);
                this.driverClassName = this.env.getProperty(this.SPRING_DRIVERCLASS);

        }

        /**
         * Setting up connection parameters from VCAP environment variables.
         *
         * @param VCAP_SERVICES
         */
        public void setDataSourcePropertiesFromVCAP(String VCAP_SERVICES) {

                JSONObject jsonObj = new JSONObject(VCAP_SERVICES);
                JSONArray jsonArray;

                jsonArray = jsonObj.getJSONArray(this.VCAP_DB_TYPE);
                // Transform the JSONArray to JSONObject because JSONArray can't find by string key
                jsonObj = jsonArray.toJSONObject(new JSONArray().put(this.VCAP_DB_TYPE));
                jsonObj = jsonObj.getJSONObject(this.VCAP_DB_TYPE);

                if (jsonObj.has(this.VCAP_DB_CREDENTIALS)) {
                        jsonObj = jsonObj.getJSONObject(this.VCAP_DB_CREDENTIALS);
                        if (jsonObj != null) {
                                this.username = jsonObj.getString(this.VCAP_DB_USER);
                                this.password = jsonObj.getString(this.VCAP_DB_PASSWORD);
                                this.url = jsonObj.getString(this.VCAP_DB_URL);
                                this.driverClassName = jsonObj.getString(this.VCAP_DB_DRIVER);
                        }
                }
        }
}
