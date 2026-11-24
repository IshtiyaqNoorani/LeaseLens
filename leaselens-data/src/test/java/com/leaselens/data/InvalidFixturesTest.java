package com.leaselens.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.leaselens.data.load.DatasetLoader;
import com.leaselens.data.load.DatasetSource;
import com.leaselens.data.load.LoadResult;
import com.leaselens.data.validation.Rule;
import com.leaselens.data.validation.Severity;

/**
 * Every folder in data/invalid holds the golden dataset with one planted defect. The loader must
 * reject each one, report the rule named in EXPECTED.txt as an ERROR with a message, and never
 * return a dataset.
 */
class InvalidFixturesTest {

    static Stream<Path> fixtures() throws Exception {
        return Files.list(TestData.dir("invalid")).filter(Files::isDirectory).sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    @DisplayName("Each planted defect is detected and blocks loading")
    void detectsPlantedDefect(Path fixture) throws Exception {
        Rule expected = Rule.valueOf(Files.readAllLines(fixture.resolve("EXPECTED.txt"), StandardCharsets.UTF_8)
                .get(0).trim());

        LoadResult result = new DatasetLoader().load(DatasetSource.directory(fixture));

        assertFalse(result.report().valid(), "fixture must be rejected");
        assertTrue(result.dataset().isEmpty(), "no partial dataset may be returned");
        assertTrue(result.report().issues().stream().anyMatch(i -> i.rule() == expected
                        && i.severity() == Severity.ERROR && i.message() != null && !i.message().isBlank()),
                () -> "expected " + expected + " but got " + result.report().issues());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("There is at least one fixture per required category of defect")
    void enoughFixtures() throws Exception {
        assertTrue(fixtures().count() >= 20);
    }
}
