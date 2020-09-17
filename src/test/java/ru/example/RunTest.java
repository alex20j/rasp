package ru.example;

import org.junit.runner.RunWith;
import cucumber.api.junit.Cucumber;
import cucumber.api.CucumberOptions;


@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/features",
        glue = "ru.example",
        tags = "@all"
)

public class RunTest {}
