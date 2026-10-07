//package com.ape.friction.integration.config;
//
//
//import jakarta.annotation.Resource;
//import org.dbunit.database.DatabaseConfig;
//import org.dbunit.database.DatabaseConnection;
//import org.dbunit.database.IDatabaseConnection;
//import org.dbunit.dataset.IDataSet;
//import org.dbunit.dataset.csv.CsvURLDataSet;
//import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
//import org.dbunit.operation.DatabaseOperation;
//import org.junit.jupiter.api.BeforeAll;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import  com.ape.friction.api.moc.services.GeocodingService;
//import  com.ape.friction.api.moc.services.PayPalClient;
//import  com.ape.friction.api.moc.services.StripeClient;
//import  com.ape.friction.api.moc.utils.CalculateWithAiService;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//
//
//public abstract class MOCIntegrationTestBase  extends IntegrationTestBase {
//
//    @Resource(name = "mocDataSource")
//    private DataSource dataSource;
//
//    @MockitoBean
//    protected PayPalClient payPalClient;
//
//    @MockitoBean
//    protected StripeClient stripeClient;
//
//    @MockitoBean
//    protected CalculateWithAiService calculateWithAiService;
//
//    @MockitoBean
//    protected GeocodingService geocodingService;
//
//    @BeforeAll
//    void setupDatabase() throws Exception {
//
//        jdbcTemplate.execute("""
//        DO $$
//        DECLARE
//            r RECORD;
//        BEGIN
//            FOR r IN (
//                SELECT tablename
//                FROM pg_tables
//                WHERE schemaname = 'public'
//            )
//            LOOP
//                EXECUTE 'TRUNCATE TABLE public.' ||
//                        quote_ident(r.tablename) ||
//                        ' RESTART IDENTITY CASCADE';
//            END LOOP;
//        END $$;
//    """);
//
//        try (Connection jdbcConnection = dataSource.getConnection()) {
//
//            IDatabaseConnection connection =
//                    new DatabaseConnection(jdbcConnection);
//
//
//            DatabaseConfig config = connection.getConfig();
//
//            config.setProperty(
//                    DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
//                    new PostgresqlDataTypeFactory()
//            );
//
//            config.setProperty(
//                    DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES,
//                    false
//            );
//            IDataSet dataSet =
//                    new CsvURLDataSet(
//                            getClass().getResource("/datasets/")
//                    );
//
//
//            DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
//        }
//    }
//}
