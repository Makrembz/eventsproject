package tn.fst.eventsproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@SpringBootTest
// Replace the real datasource with an embedded database during tests
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EventsProjectApplicationTests {

    @Test
    void contextLoads() {
    }

}
