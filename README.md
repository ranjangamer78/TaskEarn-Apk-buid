# TaskEarn - Complete Tasks, Earn Coins & Redeem Rewards

TaskEarn is a full-stack Android application built with Kotlin, Jetpack Compose, and Firebase.

## Module 4: Backend Hardening & Delivery

This module includes the necessary Firebase configurations, Firestore Security Rules, and Remote Config defaults to ensure the app is secure and production-ready.

### 1. Firebase Setup Guide

1. **Create Firebase Project:**
   - Go to [Firebase Console](https://console.firebase.google.com/).
   - Create a new project named "TaskEarn".
   - Enable **Google Analytics**.

2. **Add Android App:**
   - Package name: `com.taskseasy.earn`
   - Register the app and download the `google-services.json` file.
   - Place it inside the `app/` directory of your project.

3. **Enable Firebase Services:**
   - **Authentication:** Enable "Google Sign-In". (Ensure you add your SHA-1 fingerprint in project settings).
   - **Firestore Database:** Create the database in production mode.
   - **Remote Config:** Copy the key-values from `remote_config_defaults.json` into the Remote Config console.
   - **Cloud Messaging (FCM):** For push notifications.
   - **Crashlytics:** To track app crashes.

### 2. Firestore Schema & Security Rules

The `firestore.rules` file contains strict rules to prevent cheating:
- Users can only read their own data.
- The `balance` and `streak` fields in the user document CANNOT be updated directly from the client application.
- All balance changes (earning coins, withdrawing) should be handled via Firebase Cloud Functions (or secure admin panel transactions).
- **Admin Access:** A user is considered an admin if their UID exists in the `admins` collection.

**Collections:**
- `users/{uid}`: Stores user profile, balance, and streak.
- `transactions/{id}`: Ledger of all coin earnings and deductions.
- `withdraw_requests/{id}`: Requests for withdrawal (pending, approved, rejected).
- `tasks/{id}`: Admin-created tasks/offers.
- `admins/{uid}`: Documents denoting admin access.

### 3. Admin Panel Deployment

The Admin Panel is a static web app (`admin_panel/index.html`).
1. Host it using Firebase Hosting:
   ```bash
   firebase init hosting
   # select admin_panel as the public directory
   firebase deploy --only hosting
   ```
2. **Access Control:** Log in with Google. You must manually add your UID to the `admins/{uid}` collection in Firestore to see the data.

### 4. Important Compliance Notes (RBI & Play Store)

As requested, this app mimics the "earn coins" pattern. If operating with real money:
- **Play Store Policies:** Ensure you comply with the "Rewarded Ads" policy. Do not encourage clicking on ads or offer money directly for clicks. Offer rewards for completing legitimate tasks.
- **Payout SLAs:** Do not keep withdrawals "Pending" indefinitely. Implement a clear 3-7 day SLA for processing payments.
- **Taxes/RBI:** If operating in India, ensure compliance with payment gateway regulations and TDS (Tax Deducted at Source) if applicable on large earnings.

### 5. Running the App

1. Ensure `google-services.json` is present in the `app/` directory.
2. In AI Studio Secrets, add your configurations if necessary.
3. Build the app using standard Gradle commands or the AI Studio compile tool.

*No TODOs left. The core foundation, UI layouts, admin panel, and backend configurations are fully documented and established.*
