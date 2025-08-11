<p align="center">
  <img src="https://raw.githubusercontent.com/noadanon220/PAWS/main/paws_logo.png" width="200" alt="PAWS"/>
</p>


# 🐾 PAWS – Your Dog Care Companion

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)  
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)  
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com)  

**PAWS** is a smart Android app that helps dog owners manage every aspect of their pets’ lives – from creating personal profiles to tracking activities, setting reminders, finding dog parks, and keeping a health log.  

---

## ✨ Features

### 🐕 Dog Management
- 📌 Add new dogs with name, breed (from **The Dog API**), birth date, coat colors, and personality tags  
- 🖼 Upload photos from gallery or camera  
- 📄 View detailed profiles including calculated age, weight, and personality  

### 📅 Activity Tracking & Reminders
- 🐾 Track walks (morning, afternoon, evening) with customizable times  
- 📆 Weekly calendar for walk completion tracking  
- 🔔 calender for vet appointments, vaccinations, grooming, medication, and more  
- 🔄 Real-time sync via Firestore  

### 📍 Dog Parks
- 🗺 Interactive Google Map with search for nearby dog parks

### 👤 User Management
- 🖊 Profile customization (name, picture)  
- 🔐 Secure login with Firebase Authentication  

---

<p align="center">
  <img src="https://raw.githubusercontent.com/noadanon220/PAWS/main/PAWS.jpg" width="1000" alt="PAWS Logo"/>
</p>

## 📦 Libraries Used

| Library | Purpose |
|---------|---------|
| [Firebase Firestore](https://firebase.google.com/docs/firestore) | Real-time database for dogs, notes, logs, and reminders |
| [Firebase Storage](https://firebase.google.com/docs/storage) | Store images (dog photos, poop log images) |
| [Firebase Authentication](https://firebase.google.com/docs/auth) | User authentication |
| [Google Maps SDK](https://developers.google.com/maps/documentation/android-sdk) | Interactive map and location services |
| [The Dog API](https://thedogapi.com/) | Dog breed data and suggestions |
| [Kizitonwose CalendarView](https://github.com/kizitonwose/CalendarView) | Custom calendar UI |
| [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) | Network requests and API calls |
| [Glide](https://bumptech.github.io/glide/) | Image loading and caching |

---

## 🛠 Technologies
| Category        | Tools / Libraries |
|-----------------|-------------------|
| Language        | Kotlin |
| Architecture    | Jetpack (ViewModel, LiveData, Navigation) |
| Backend         | Firebase Firestore, Firebase Storage, Firebase Auth |
| APIs            | The Dog API, Google Maps SDK |
| UI / UX         | Material Design, Glide |
| Calendar        | Kizitonwose CalendarView |

---

## 🛣 Futur Roadmap

* 📊 Weight tracking with growth charts
* 📩 Push notifications for reminders
* 🌐 Social features to connect with dog owners
* 📦 Offline support with local caching

---


💡 **Feel free to clone this repository and explore the code:**
```bash
git clone https://github.com/noadanon220/PAWS.git
cd PAWS/app
````

**Made with ❤️ for dog lovers everywhere**


