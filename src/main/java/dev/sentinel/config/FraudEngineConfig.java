package dev.sentinel.config;

import dev.sentinel.engine.FraudEngine;
import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.rules.BlacklistRule;
import dev.sentinel.engine.rules.CardTestingRule;
import dev.sentinel.engine.rules.CountryChangeRule;
import dev.sentinel.engine.rules.HighAmountRule;
import dev.sentinel.engine.rules.NewDeviceRule;
import dev.sentinel.engine.rules.VelocityRule;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FraudEngineConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FraudEngine fraudEngine(FraudProperties p) {
        List<FraudRule> rules = List.of(
                new HighAmountRule(p.highAmount(), p.veryHighAmount(), 40, 80),
                new VelocityRule(p.velocityMaxCount(), p.velocityWindow(), 35),
                new CountryChangeRule(p.countryWindow(), 45),
                new BlacklistRule(100),
                new NewDeviceRule(p.newDeviceMinAmount(), 25),
                new CardTestingRule(p.cardTestingCount(), p.cardTestingMaxAmount(), p.cardTestingWindow(), 40));
        return new FraudEngine(rules, p.reviewThreshold(), p.declineThreshold());
    }
}
