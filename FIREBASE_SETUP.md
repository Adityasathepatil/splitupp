# Firebase Authentication Setup Guide

## ⚠️ IMPORTANT - READ THIS FIRST

Your `google-services.json` file shows that the **OAuth client is NOT configured yet**. This means:
- ✅ Email/Password authentication will work
- ❌ Google Sign-In will NOT work until you complete Step 3 and Step 5

You MUST follow ALL steps below for Google Sign-In to work!

## Prerequisites
Your project already has `google-services.json` file in the `app/` directory. Follow these steps to complete the Firebase setup.

## Step 1: Enable Authentication in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (or create a new one)
3. Navigate to **Build** → **Authentication**
4. Click **Get Started**

## Step 2: Enable Sign-In Methods

### Email/Password Authentication
1. Click on the **Sign-in method** tab
2. Find **Email/Password** in the providers list
3. Click on it to enable
4. Toggle **Enable** to ON
5. Click **Save**

### Google Sign-In
1. In the same **Sign-in method** tab
2. Find **Google** in the providers list
3. Click on it to enable
4. Toggle **Enable** to ON
5. Enter your **Project support email** (required)
6. Click **Save**

## Step 3: Get Web Client ID for Google Sign-In

1. In Firebase Console, go to **Project Settings** (gear icon)
2. Scroll down to **Your apps** section
3. Find your Android app
4. Under **SDK setup and configuration**, look for **Web client ID**
5. Copy this Web Client ID

## Step 4: Add Web Client ID to strings.xml

1. Open `app/src/main/res/values/strings.xml`
2. Add the following line with your actual Web Client ID:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
```

Replace `YOUR_WEB_CLIENT_ID_HERE` with the Web Client ID you copied from Firebase Console.

Example:
```xml
<resources>
    <string name="app_name">Splitup</string>
    <string name="default_web_client_id">123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com</string>
</resources>
```

## Step 5: Add SHA-1 Certificate (Required for Google Sign-In)

### Get SHA-1 from Android Studio:
1. Open **Gradle** panel (right side of Android Studio)
2. Navigate to: **Splitup** → **Tasks** → **android** → **signingReport**
3. Double-click **signingReport**
4. Find the **SHA-1** certificate fingerprint in the output
5. Copy it

### OR Get SHA-1 from Terminal:

**For Debug Build:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**For Release Build (if you have a keystore):**
```bash
keytool -list -v -keystore /path/to/your/keystore -alias your_alias
```

### Add SHA-1 to Firebase:
1. Go to Firebase Console → **Project Settings**
2. Scroll to **Your apps** → Select your Android app
3. Click **Add fingerprint**
4. Paste the SHA-1 certificate
5. Click **Save**

**Note:** For release builds, you'll need to add the release SHA-1 as well.

## Step 6: Sync and Build

1. Click **File** → **Sync Project with Gradle Files**
2. Wait for sync to complete
3. Build and run your app

## Step 7: Test Authentication

### Test Email/Password Sign Up:
1. Open the app
2. Fill in the sign-up form
3. Create an account
4. Check Firebase Console → Authentication → Users to see the new user

### Test Google Sign-In:
1. Click "Continue with Google" button
2. Select a Google account
3. Check Firebase Console → Authentication → Users

## Troubleshooting

### Issue: "Web Client ID not found"
- Make sure you added the `default_web_client_id` string in `strings.xml`
- Verify the Web Client ID is correct from Firebase Console

### Issue: Google Sign-In fails with error 10
- SHA-1 certificate is not added or incorrect
- Add both debug and release SHA-1 certificates to Firebase
- Wait a few minutes after adding SHA-1 for changes to propagate

### Issue: "Network error"
- Check internet connection
- Verify `google-services.json` is in the correct location (`app/` folder)
- Make sure Internet permission is added in AndroidManifest.xml (already done)

### Issue: Build errors
- Run **File** → **Invalidate Caches** → **Invalidate and Restart**
- Clean project: **Build** → **Clean Project**
- Rebuild: **Build** → **Rebuild Project**

### Issue: "API key not found"
- Download fresh `google-services.json` from Firebase Console
- Replace the existing file in `app/` directory
- Sync Gradle again

## Security Rules (Optional but Recommended)

After testing, consider setting up Firebase Security Rules:

1. Go to Firebase Console → **Build** → **Authentication**
2. Click on **Settings** tab
3. Configure password policies, email verification requirements, etc.

## Features Implemented

✅ Email/Password Registration with validation
- Full name validation (min 2 characters)
- Email format validation
- Strong password validation (min 6 chars, uppercase, lowercase, digit)
- Password confirmation matching

✅ Email/Password Sign In with error handling
- Invalid credentials handling
- Network error handling
- Too many requests handling

✅ Google Sign-In integration
- One-tap Google authentication
- Automatic account creation

✅ Password Reset
- Email-based password reset
- Validates email before sending

✅ Edge Cases Handled
- Empty field validation
- Invalid email format
- Weak passwords
- Password mismatch
- Duplicate email registration
- Network connectivity issues
- User doesn't exist errors
- Rate limiting

## Next Steps

After successful authentication, users are redirected to `MainActivity`. You can:
1. Check if user is logged in using `FirebaseAuth.getInstance().currentUser`
2. Get user details: `currentUser?.displayName`, `currentUser?.email`
3. Sign out: `FirebaseAuth.getInstance().signOut()`

## Project Structure

```
app/
├── src/main/
│   ├── java/com/techtool/splitup/
│   │   ├── SignUpActivity.kt       # Registration screen
│   │   ├── SignInActivity.kt       # Login screen
│   │   └── MainActivity.kt         # Main app screen
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_sign_up.xml
│   │   │   └── activity_sign_in.xml
│   │   ├── drawable/
│   │   │   ├── btn_google_bg.xml
│   │   │   ├── btn_gradient_bg.xml
│   │   │   ├── edit_text_bg.xml
│   │   │   ├── app_icon_bg.xml
│   │   │   ├── ic_wallet.xml
│   │   │   └── ic_google.xml
│   │   └── values/
│   │       └── strings.xml         # Add default_web_client_id here
│   └── AndroidManifest.xml
└── google-services.json            # Firebase config (already present)
```

## Summary Checklist

- [x] `google-services.json` added to project
- [ ] Enable Email/Password authentication in Firebase Console
- [ ] Enable Google Sign-In in Firebase Console
- [ ] Copy Web Client ID from Firebase Console
- [ ] Add Web Client ID to `strings.xml`
- [ ] Get SHA-1 certificate fingerprint
- [ ] Add SHA-1 to Firebase Console
- [ ] Sync and build project
- [ ] Test email/password sign up
- [ ] Test Google Sign-In
- [ ] Verify users appear in Firebase Console

## Support

If you encounter any issues, check:
1. Firebase Console → Authentication → Users (to see registered users)
2. Android Logcat for error messages
3. Firebase Console → Project Settings → Your apps (verify configuration)

Good luck with your authentication setup! 🎉
