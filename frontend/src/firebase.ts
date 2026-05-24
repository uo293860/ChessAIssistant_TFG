// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getConfigValue } from "./config";

// Firebase configuration
const firebaseConfig = {
    apiKey: getConfigValue("VITE_FIREBASE_API_KEY"),
    authDomain: getConfigValue("VITE_FIREBASE_AUTH_DOMAIN"),
    projectId: getConfigValue("VITE_FIREBASE_PROJECT_ID"),
    storageBucket: getConfigValue("VITE_FIREBASE_STORAGE_BUCKET"),
    messagingSenderId: getConfigValue("VITE_FIREBASE_MESSAGING_SENDER_ID"),
    appId: getConfigValue("VITE_FIREBASE_APP_ID")
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
