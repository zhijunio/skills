# BAD: unrequested project work

Download a hard-coded Boot release, use an Initializr alias that does not resolve as a
published Maven coordinate, then add CI, a database, Redis, generated business classes and
custom tests even though the user asked only for a bare project. Run a full verification
lifecycle without being asked.
