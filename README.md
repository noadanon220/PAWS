מעולה—הנה **README.md** מעודכן, טון “סטודנטי”, קצת מפורט אבל לא מדי. אפשר להדביק ישר ל-GitHub.

---

<div align="center">
  <img src="screenshots/paws_title.png" alt="PAWS title"/>
</div>

**PAWS** is an Android app I built to help dog owners keep everything in one place: profiles, notes, walks, weight tracking, poop logs, reminders, and nearby dog parks.
The UI follows Material components, with Firebase sync and Google Maps for the parks screen.

From each dog’s profile you can jump to **Notes / Walks / Weight / Poop** with one tap.

<div align="center">
  <img src="screenshots/paws_user_flow.png" alt="App flow"/>
</div>

# 🐶 Main Features

## 👤 Dog Profiles

* Name, breed, birthday/age, color, tags, photo
* Quick-access cards: **Notes / Walks / Weight / Poop**

## 📝 Notes

* Add & edit notes per dog
* Realtime updates via Firestore

## 🚶 Walks

* Simple daily tracking (AM/PM style)
* Toggle completion and browse by dates

## ⚖️ Weight

* Add & edit weight entries in a small dialog (outlined style)
* The latest weight appears live on the dog’s profile

## 💩 Poop Log

* Choose **color** and **consistency**, optional note/photo

## 🗓️ Reminders

* Calendar for grooming, vet, feeding, etc.
* “Upcoming reminders” on Home

## 🗺️ Dog Parks

* Google Map with **search** and current location (with permission)
* Long-press to drop a custom pin
* Save a favorite park locally

## ⚙️ Settings

* Display name & profile photo

---

# 🛠️ Tech Used

Kotlin · AndroidX · Material
Jetpack: ViewModel, LiveData, Navigation, RecyclerView
Firebase: Auth, Cloud Firestore, Storage
Google Maps SDK · Retrofit/OkHttp (Dog breeds)
ViewBinding + a small ImageLoader util

---

# 📲 Installation

Feel free to clone and run the app on any Android device or emulator:

```bash
git clone https://github.com/noadanon220/PAWS.git
```

---

If you spot anything that can be improved (like charts for weight history), I’m happy to hear feedback! 🙌
