# Testing Guide

This guide covers running tests for the Slack Consultation Skill.

## Test Structure

```
shared/src/test/kotlin/
├── com/claude/slack/shared/
│   ├── config/
│   │   └── ConfigManagerTest.kt        # Configuration loading tests
│   ├── health/
│   │   └── HealthClientTest.kt         # Health client tests
│   ├── state/
│   │   ├── StateManagerTest.kt         # FileStateManager tests
│   │   └── MongoStateManagerTest.kt    # MongoDB integration tests
│   └── util/
│       └── RetryHelperTest.kt          # Retry logic tests

server/src/test/kotlin/
└── com/claude/slack/server/
    └── health/
        └── HealthServerTest.kt         # Health endpoint tests
```

## Running Tests

### All Tests

```bash
./gradlew test
```

### Module-Specific Tests

```bash
# Shared module
./gradlew :shared:test

# Server module
./gradlew :server:test

# CLI module
./gradlew :cli:test
```

### Specific Test Classes

```bash
# Configuration tests
./gradlew :shared:test --tests "*ConfigManagerTest*"

# State manager tests
./gradlew :shared:test --tests "*StateManagerTest*"

# MongoDB integration tests
./gradlew :shared:test --tests "*MongoStateManagerTest*"

# Retry helper tests
./gradlew :shared:test --tests "*RetryHelperTest*"

# Health client tests
./gradlew :shared:test --tests "*HealthClientTest*"
```

## Integration Tests with MongoDB

MongoDB integration tests use [Testcontainers](https://www.testcontainers.org/) to spin up a real MongoDB instance in Docker.

### Prerequisites

- Docker must be running
- First run may take longer (downloads MongoDB image)

### Running MongoDB Tests

```bash
# Run MongoDB integration tests
./gradlew :shared:test --tests "*MongoStateManagerTest*"
```

### Test Coverage

MongoDB tests cover:
- CRUD operations for consultation requests
- Heartbeat read/write
- Latest pending query
- User-specific queries
- Connection status checks
- Index creation

## Unit Tests

Unit tests run without external dependencies:

```bash
# Fast unit tests only (excludes integration tests)
./gradlew :shared:test --tests "*Test" --exclude-task testcontainers

# Or run specific fast tests
./gradlew :shared:test --tests "*RetryHelperTest*"
./gradlew :shared:test --tests "*ConfigManagerTest*"
./gradlew :shared:test --tests "*HealthClientTest*"
```

## Manual Testing Checklist

### Server Startup

- [ ] Server starts without errors
- [ ] Health endpoint responds at `/health`
- [ ] MongoDB connection established
- [ ] Heartbeat being written
- [ ] Slack Socket Mode connected

### CLI Health Check

- [ ] CLI detects when server is not running
- [ ] CLI waits and retries when server is starting
- [ ] CLI proceeds when server is healthy
- [ ] Stale heartbeat warnings displayed

### Ask Command Flow

- [ ] User resolution works (by username, email, alias)
- [ ] Question sent to Slack successfully
- [ ] Request stored in MongoDB
- [ ] Request ID displayed

### Check Command Flow

- [ ] Latest pending request retrieved
- [ ] Specific request retrieved by ID
- [ ] Response displayed when answered
- [ ] Expiration handled correctly

### Blocking Mode

- [ ] `--wait` flag works
- [ ] Timeout after specified duration
- [ ] Response received during wait
- [ ] Heartbeat monitoring during wait

### Health Endpoints

```bash
# Test all health endpoints
curl http://localhost:8080/health
curl http://localhost:8080/health/live
curl http://localhost:8080/health/ready
curl http://localhost:8080/health/heartbeat
```

## CI/CD Configuration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      mongo:
        image: mongo:6.0
        ports:
          - 27017:27017

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission
      run: chmod +x gradlew

    - name: Run tests
      run: ./gradlew test

    - name: Upload test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: '**/build/reports/tests/'
```

### GitLab CI Example

```yaml
test:
  image: gradle:8.5-jdk17
  services:
    - mongo:6.0
  variables:
    MONGODB_CONNECTION_STRING: "mongodb://mongo:27017"
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: '**/build/test-results/test/*.xml'
```

## Test Reports

After running tests, view reports at:

- `shared/build/reports/tests/test/index.html`
- `server/build/reports/tests/test/index.html`
- `cli/build/reports/tests/test/index.html`

## Debugging Tests

### Enable Debug Logging

```bash
./gradlew test --info
```

### Run Single Test with Output

```bash
./gradlew :shared:test --tests "*MongoStateManagerTest.should add and retrieve*" --info
```

### Testcontainers Debugging

If Testcontainers tests fail:

1. Ensure Docker is running
2. Check Docker has enough resources
3. Try pulling the image manually: `docker pull mongo:6.0`
4. Check Testcontainers logs in test output

## Writing New Tests

### Unit Test Template

```kotlin
class MyFeatureTest {
    @Test
    fun `should do something`() {
        // Arrange
        val input = "test"

        // Act
        val result = myFunction(input)

        // Assert
        assertEquals("expected", result)
    }
}
```

### Integration Test with MongoDB

```kotlin
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyMongoTest {
    companion object {
        @Container
        @JvmStatic
        val mongoContainer = MongoDBContainer("mongo:6.0")
    }

    private lateinit var stateManager: MongoStateManager

    @BeforeEach
    fun setup() {
        stateManager = MongoStateManager(
            connectionString = mongoContainer.connectionString,
            databaseName = "test_${UUID.randomUUID().toString().take(8)}"
        )
    }

    @Test
    fun `should interact with MongoDB`() {
        // Test code here
    }
}
```
