## OPT Category Format Guidelines

When adding entries for the **OPT** category to the module’s `data.xml`, follow the rules below:

### 1. Category Name
- Must be **unique**.
- Must **not** contain **symbols** or **special characters**.

### 2. Application ID
- The `Application_ID` must refer to a valid record in the `MOTPApplication` table.

### 3. CONFIG Attribute Format

Use the following format for the `CONFIG` attribute in `data.xml`:

```xml
CONFIG="{&quot;resendDuration&quot; : &quot;30&quot;, &quot;maxAllowedAttempts&quot; : 30, &quot;lockoutDurationAfterMaxAttempts&quot; : &quot;60&quot;}"

C> 💡 CONFIG Field Descriptions

| Field                           | Description                                                             |
|---- --------------------------  |-------------------------------------------------------------------------|
| resendDuration                  | Time in seconds before a new OTP can be resent.                         |
| maxAllowedAttempts              | Maximum number of allowed OTP attempts.                                 |
| lockoutDurationAfterMaxAttempts | Time in seconds the user is locked out after exceeding allowed attempts. |
