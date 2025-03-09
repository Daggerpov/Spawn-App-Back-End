# Push Notification Setup

## Apple Push Notification Service (APNS) Setup

1. Log in to your Apple Developer account at https://developer.apple.com/
2. Go to "Certificates, Identifiers & Profiles"
3. Create a new App ID with Push Notifications capability enabled
4. Create a new Push Notification certificate (Development or Production)
5. Download the certificate and double-click to add it to your Keychain
6. Export the certificate as a .p12 file (you'll be prompted to set a password)
7. Convert the .p12 file to Base64 format using the following command:
   ```
   cat apns-cert.p12 | base64 | tr -d '\n' > apns-cert-base64.txt
   ```
8. Set the Base64 encoded certificate as an environment variable named `APNS_CERTIFICATE`
9. Set the certificate password as an environment variable named `CERTIFICATE_PASSWORD`

## Firebase Cloud Messaging (FCM) Setup

1. Go to the Firebase Console at https://console.firebase.google.com/
2. Create a new project or use an existing one
3. Add an Android app to your project
4. Download the Firebase Admin SDK service account key (JSON file)
5. Place the JSON file in the resources directory as `firebase-config.json`

## Configuration

The application is configured to use environment variables for sensitive information:

```properties
# APNS Configuration for iOS
apns.certificate.path=${APNS_CERTIFICATE}
apns.certificate.password=${CERTIFICATE_PASSWORD}
apns.production=true  # Set to false for development

# Firebase Configuration for Android
firebase.config.path=firebase-config.json
``` 