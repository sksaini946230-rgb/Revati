# Admin panel — owner setup (one time)

Chaar kaam console me, aapke haath se. Maine kuch nahi badla hai. Har step ke
baad screenshot bhejna ho to bhej dena.

## 0. Admin emails ki list (Firestore me, repo me nahi)

Ye repo public hai, isliye emails yahan nahi likhi. List Claude ne chat me di hai.

1. https://console.firebase.google.com → project **astroveda-7126b** →
   **Firestore Database** → **Data** tab.
2. **+ Start collection** → Collection ID: `config` → Next.
3. Document ID: `admins`.
4. Field: `emails`, Type: **array**. Har email ek item, chhote akshar (lower case)
   me. Aathon daalo → **Save**.

Nayi email jodni ho to isi array me jod do; hatani ho to hata do. Bas.

## 1. Firestore rules daalna (5 minute)

1. https://console.firebase.google.com → project **astroveda-7126b**.
2. Left menu → **Firestore Database** → upar **Rules** tab.
3. Jo likha hai sab select karke hata do.
4. Is repo ki file `firebase/firestore.rules` ka poora text paste karo.
5. **Publish** dabao.

Purani app par iska koi asar nahi padta: users ka backup pehle ki tarah chalta
rahega.

## 2. Identity Platform upgrade (2-step code ke liye)

1. Usi project me left menu → **Authentication** → **Settings** tab.
2. Wahan **Upgrade to Firebase Authentication with Identity Platform** ka button
   hoga → **Upgrade**.
3. Free (Spark) plan par ye free hai, 3,000 daily users tak. **Agar card ya
   billing maange to wahin ruk jaana aur mujhe batana.**

## 3. Authenticator code chalu karna (ek command)

1. https://console.cloud.google.com → upar project **astroveda-7126b** chuno.
2. Upar-daayein **>_** icon (Activate Cloud Shell) → neeche terminal khulega.
   Authorize maange to Authorize.
3. Ye poori line paste karke Enter:

```
curl -X PATCH "https://identitytoolkit.googleapis.com/admin/v2/projects/astroveda-7126b/config?updateMask=mfa" -H "Authorization: Bearer $(gcloud auth print-access-token)" -H "Content-Type: application/json" -H "X-Goog-User-Project: astroveda-7126b" -d '{"mfa":{"providerConfigs":[{"state":"ENABLED","totpProviderConfig":{"adjacentIntervals":5}}]}}'
```

4. Jawab me `"totpProviderConfig"` dikhe to ho gaya. Error aaye to screenshot bhejo.

## 4. Build ke baad, phone par

1. Admin email se sign in → Settings → **Admin panel**.
2. **Set up the authenticator** → Google Authenticator me Revati jud jaayega →
   jo 6 digit code dikhe, daalo → Confirm.
3. App bolega sign out karke dobara sign in karo. Sign in karne par code
   maangega → daalo → panel khul jaayega.
4. Google Authenticator me **cloud backup ON** rakho: phone kho gaya to admin
   bhi kho jaayega.

## 5. Play Console — Data safety

Nayi build me Firebase Cloud Messaging hai. Data safety form me **Device or
other IDs** (App functionality ke liye, share nahi hota) jodna padega. Build
upload karte waqt ye kaam aapka hai.

## Sabko message kaise bhejein

Admin panel → **Message** tab → **Open Messaging** → New campaign → Firebase
Notification messages → title aur text → app `com.aistudio.astroveda.kpvqzm`
→ Now → Publish. Sirf nayi build wale phones par pahunchega.
