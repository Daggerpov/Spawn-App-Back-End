# Push Notification Setup

## Apple Push Notification Service (APNS) Setup

1. Log in to your Apple Developer account at https://developer.apple.com/
2. Go to "Certificates, Identifiers & Profiles"
3. Create a new App ID with Push Notifications capability enabled
4. Create a new Push Notification certificate (Development or Production)
5. Download the certificate and double-click to add it to your Keychain
6. Export the certificate as a .p12 file (you'll be prompted to set a password)
7. Place the .p12 file in this directory with the name `apns-cert.p12`
8. Update the password in `application.properties` to match the one you set

## Firebase Cloud Messaging (FCM) Setup

1. Go to the Firebase Console at https://console.firebase.google.com/
2. Create a new project or use an existing one
3. Add an Android app to your project
4. Download the Firebase Admin SDK service account key (JSON file)
5. Place the JSON file in the resources directory as `firebase-config.json`

## Configuration

Make sure to update the following properties in `application.properties`:

```properties
# APNS Configuration for iOS
apns.certificate.path=classpath:certificates/apns-cert.p12
apns.certificate.password=your_certificate_password
apns.production=false  # Set to true for production

# Firebase Configuration for Android
firebase.config.path=firebase-config.json
``` 