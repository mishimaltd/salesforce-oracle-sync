package com.mishima.sync.instant;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class InstantTest {

  @Test
  public void testGenerateUTCDate() {
    Instant midnightUtc = Instant.now().truncatedTo(ChronoUnit.DAYS);
    log.info("Midnight: {}", midnightUtc);
    Date date = Date.from(midnightUtc);
    log.info("Date: {}", date);

    Instant start = Instant.now().minus(Period.ofDays(1)).atZone(ZoneId.of("UTC")).toInstant().truncatedTo(ChronoUnit.DAYS);
    Instant end = start.plus(Period.ofDays(1));

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a").withZone(ZoneId.of("UTC"));
    String fromStr = fmt.format(start);
    String toStr = fmt.format(end);
    log.info("{} -> {}", fromStr, toStr);

  }

}
