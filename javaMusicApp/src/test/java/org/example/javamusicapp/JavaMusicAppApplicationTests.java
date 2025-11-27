package org.example.javamusicapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = { "app.frontend.url=http://localhost:3000" })
class JavaMusicAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
