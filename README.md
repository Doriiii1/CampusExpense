# Campus Expense Tracker

A comprehensive Android expense tracking application for campus students, built with Java and Android SDK.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Build & Run](#build--run)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Project Structure](#project-structure)
- [Development](#development)
- [Contributing](#contributing)

---

## ✨ Features

### Phase 1-2: Core Authentication & Expense Management
- ✅ **Secure Authentication**: User registration and login with SHA-256 password hashing and per-user salt
- ✅ **Session Management**: 30-minute timeout with automatic logout
- ✅ **Expense Tracking**: Add, edit, delete, and view expenses with categories
- ✅ **Multi-user Support**: Complete user isolation for expenses and budgets
- ✅ **SQLite Database**: Persistent local storage

### Phase 3: Recurring Expenses
- ✅ **Recurring Expenses**: Set up daily, weekly, or monthly recurring expenses
- ✅ **Background Processing**: WorkManager-based recurring expense insertion
- ✅ **Notifications**: Reminders for recurring expenses
- ✅ **Budget Integration**: Automatic budget updates when recurring expenses are inserted

### Phase 4: Reports & Visualizations
- ✅ **Category Reports**: Pie charts showing spending distribution by category
- ✅ **Time-based Reports**: Line charts for spending trends over time
- ✅ **Budget Progress**: Visual indicators for budget thresholds (80%, 100%)
- ✅ **Export Capabilities**: Share and export reports

### Phase 5: Budget Management
- ✅ **Budget Creation**: Set spending limits per category
- ✅ **Threshold Alerts**: Notifications at 80% and 100% of budget
- ✅ **Real-time Tracking**: Live budget updates as expenses are added
- ✅ **Cycle Support**: Monthly, weekly, and custom budget cycles

### Phase 6: Testing & CI
- ✅ **Unit Tests**: Comprehensive tests for business logic (AuthManager, DatabaseHelper)
- ✅ **Integration Tests**: Recurring expense flow testing
- ✅ **UI Tests**: Espresso tests for critical user flows
- ✅ **Code Coverage**: ≥60% coverage for business logic enforced by CI
- ✅ **GitHub Actions CI**: Automated testing on every push/PR

---

## 🏗️ Architecture
```
com.example.campusexpense/
├── auth/              # Authentication logic (AuthManager)
├── db/                # Database layer (DatabaseHelper, migrations)
├── model/             # Data models (Expense, Budget, RecurringExpense)
├── ui/                # Activities and UI components
├── schedule/          # Recurring expense processing (RecurringManager, Workers)
├── notifications/     # Notification system
├── reports/           # Report generation and visualization
├── adapters/          # RecyclerView adapters
└── test/              # Test utilities (TestAppInjector)
```

### Key Components

- **AuthManager**: Handles user registration, login, session management
- **DatabaseHelper**: SQLite operations, CRUD for all entities
- **RecurringManager**: Processes due recurring expenses and inserts them
- **NotificationHelper**: Sends notifications for budgets and recurring expenses
- **WorkManager**: Schedules periodic checks for recurring expenses

---

## 📦 Requirements

### Development
- **Android Studio**: Electric Eel (2022.1.1) or later
- **JDK**: 17 or later
- **Android SDK**: API 29-33 (Android 10-13)
- **Gradle**: 8.0+ (via wrapper)

### Runtime
- **Min SDK**: API 29 (Android 10)
- **Target SDK**: API 33 (Android 13)

### Dependencies
```gradle
// Core Android
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.9.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// WorkManager for recurring expenses
implementation 'androidx.work:work-runtime:2.8.1'

// MPAndroidChart for visualizations
implementation 'com.github.PhilJay:MPAndroidChart:3.1.0'

// Testing
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:3.12.4'
testImplementation 'org.robolectric:robolectric:4.10.3'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

---

## 🚀 Build & Run

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/campus-expense-tracker.git
cd campus-expense-tracker
```

### 2. Build Project
```bash
./gradlew assembleDebug
```

### 3. Install on Device/Emulator
```bash
./gradlew installDebug
```

### 4. Run Application
- Launch from Android Studio: Click "Run" button
- Or via ADB:
```bash
adb shell am start -n com.example.campusexpense/.ui.LoginActivity
```

---

## 🧪 Testing

### Unit Tests

Run all unit tests:
```bash
./gradlew testDebugUnitTest
```

Run specific test class:
```bash
./gradlew test --tests com.example.campusexpense.auth.AuthManagerTest
```

View test report:
```bash
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Instrumentation Tests (Espresso)

**Prerequisites**: Running emulator or connected device

Run all instrumentation tests:
```bash
./gradlew connectedDebugAndroidTest
```

Run specific test:
```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.campusexpense.ui.LoginActivityEspressoTest
```

View test report:
```bash
open app/build/reports/androidTests/connected/index.html
```

### Code Coverage

Generate combined coverage report (unit + instrumentation):
```bash
./gradlew jacocoTestReportMerged
```

View coverage report:
```bash
open app/build/reports/jacoco/jacocoTestReportMerged/html/index.html
```

### Test Utilities

**TestAppInjector** - For dependency injection in tests:
```java
// In test setUp()
TestAppInjector.setTestMode(true);
TestAppInjector.setNowMillis(fixedTimestamp);
TestAppInjector.setDatabaseHelper(testDbHelper);

// In test tearDown()
TestAppInjector.reset();
```

**RecurringManager.runNowForTesting()** - Trigger recurring processing immediately:
```java
RecurringManager manager = new RecurringManager(context);
manager.runNowForTesting(); // Processes due recurring expenses synchronously
```

---

## 🔄 CI/CD

### GitHub Actions Workflow

Automated testing runs on every push and pull request:

**Jobs:**
1. **Build**: Compile debug APK
2. **Unit Tests**: Run all JUnit tests
3. **Instrumentation Tests**: Run Espresso tests on emulator (API 29, 33)
4. **Coverage**: Generate and enforce ≥60% business logic coverage
5. **Artifacts**: Upload test results and coverage reports

### Workflow File

`.github/workflows/android-ci.yml`

### Coverage Enforcement

CI fails if business logic coverage (packages: `auth`, `db`, `schedule`, `notifications`, `model`) falls below **60%**.

Coverage is calculated from:
- `app/build/reports/jacoco/jacocoTestReportMerged/jacocoTestReportMerged.xml`

### Running CI Locally

Simulate CI environment:
```bash
# Build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Start emulator (API 29 recommended)
emulator -avd Pixel_4_API_29 -no-snapshot-save -no-window -no-audio

# Wait for boot
adb wait-for-device

# Instrumentation tests
./gradlew connectedDebugAndroidTest

# Coverage
./gradlew jacocoTestReportMerged

# Check threshold (manual)
python3 scripts/check_coverage.py
```

---

## 📁 Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/campusexpense/
│   │   │   ├── auth/              # Authentication
│   │   │   ├── db/                # Database
│   │   │   ├── model/             # Data models
│   │   │   ├── ui/                # Activities
│   │   │   ├── schedule/          # Recurring logic
│   │   │   ├── notifications/     # Notifications
│   │   │   ├── reports/           # Reports
│   │   │   ├── adapters/          # Adapters
│   │   │   └── test/              # Test utilities
│   │   ├── res/                   # Resources
│   │   └── AndroidManifest.xml
│   ├── test/                       # Unit tests
│   │   └── java/com/example/campusexpense/
│   │       ├── auth/
│   │       ├── db/
│   │       └── schedule/
│   └── androidTest/                # Instrumentation tests
│       └── java/com/example/campusexpense/
│           └── ui/
├── build.gradle                    # Module build config
└── coverage/
    └── coverage-report-placeholder.txt
```

---

## 🛠️ Development

### Adding a New Feature

1. **Create model** (if needed) in `model/`
2. **Update database schema** in `DatabaseHelper.java`
3. **Add business logic** in appropriate package
4. **Create UI** in `ui/`
5. **Write tests**:
   - Unit tests in `src/test/`
   - UI tests in `src/androidTest/`
6. **Run tests locally**
7. **Submit PR** (CI will run automatically)

### Code Style

- **Language**: Java 8
- **Formatting**: Follow Android Code Style (4-space indentation)
- **Naming**:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Resources: `snake_case`

### Database Migrations

When modifying schema:
1. Increment `DATABASE_VERSION` in `DatabaseHelper`
2. Implement migration in `onUpgrade()`
3. Add migration test in `DatabaseHelperTest`

---

## 🧪 QA Checklist

### Critical (P0)
- [ ] Unit tests pass locally: `./gradlew testDebugUnitTest`
- [ ] CI workflow completes successfully
- [ ] Business logic coverage ≥ 60%

### High Priority (P1)
- [ ] Espresso tests pass locally: `./gradlew connectedDebugAndroidTest`
- [ ] RecurringManager integration tests pass
- [ ] No regressions in existing features

### Medium Priority (P2)
- [ ] Test data fixtures load correctly
- [ ] README reflects latest changes
- [ ] Coverage report accessible in CI artifacts

---

## 📊 Test Coverage Goals

| Package | Target Coverage | Current |
|---------|----------------|---------|
| `auth` | ≥70% | ✅ |
| `db` | ≥70% | ✅ |
| `schedule` | ≥65% | ✅ |
| `notifications` | ≥60% | ✅ |
| `model` | ≥50% | ✅ |
| **Overall Business Logic** | **≥60%** | **✅** |

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open Pull Request

### PR Requirements
- [ ] All tests pass
- [ ] Code coverage maintained or improved
- [ ] No lint errors
- [ ] README updated (if applicable)

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.

---

## 🙋 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/campus-expense-tracker/issues)
- **Documentation**: This README
- **API Docs**: See inline Javadoc comments

---

## 📝 Changelog

### Phase 6 (Current)
- ✅ Added comprehensive test suite
- ✅ Implemented GitHub Actions CI
- ✅ JaCoCo coverage reporting
- ✅ Test utilities and helpers

### Phase 5
- ✅ Budget management system
- ✅ Threshold notifications

### Phase 4
- ✅ Reports and visualizations
- ✅ MPAndroidChart integration

### Phase 3
- ✅ Recurring expenses
- ✅ WorkManager background processing

### Phase 1-2
- ✅ Core authentication
- ✅ Expense CRUD operations

---

**Built with ❤️ for campus students**
