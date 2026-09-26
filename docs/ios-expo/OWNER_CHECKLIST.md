# Aapse kya-kya chahiye (Owner checklist)

Ye list samay ke hisaab se hai. Jis charan ke saamne jo likha hai, wo us charan
se pehle chahiye. Console aur browser me click **aap** karoge, main har kadam
bataunga. Phone par check bhi **aap** karoge.

> **Kabhi chat me mat bhejna:** koi password, OTP, ya `.p8` / `.jks` key ka
> andar ka text. Aisi file `~/secure-credentials/revati/` folder me rakh do aur
> sirf uska naam batao. Jahan password daalna ho, wahan terminal khud aapse
> poochhega.

---

## A. Abhi — plan ke faisle (kuch bhi shuru hone se pehle)

| # | Faisla | Mera sujhav |
|---|---|---|
| A1 | Ye poora plan (`README.md`) manzoor hai? | — |
| A2 | ✅ **Tay ho gaya: `app.revati.jyotish`** (20 Sep 2026). iPhone app ki **Bundle ID**. Ye ek baar lagne ke baad kabhi nahi badalti. | Yahi rakho |
| A3 | Apple account **Individual** hai, isliye App Store par seller ke roop me **aapka apna naam** dikhega, "Msunjay Enterprises" nahi. Company ka naam chahiye to Organization account (D-U-N-S number ke saath) lena padega. | Abhi Individual se shuru karo, company baad me |
| A4 | iPhone par bhi **ads** dikhein? | Haan, Android jaise hi |
| A5 | iPhone par bhi **PRO ₹199/saal**? | Haan |
| A6 | PRO ki kharid kaise check ho: **RevenueCat** (server par pakki jaanch, nakli PRO nahi chalega) ya seedha Apple/Google se? | RevenueCat |
| A7 | Android ka design wahi rahe ya Android ka bhi naya design? | Abhi wahi rahe |
| A8 | App kin deshon me: sirf **India**, ya poori duniya? | Shuru me India |
| A9 | App Store category: **Lifestyle** (aur doosri **Reference**)? | Haan |
| A10 | GitHub par naya **private** repo `Revati-Expo` main bana doon? | Haan bolo, main bana dunga |
| A11 | Naye app me **gold ki jagah copper-orange `#E8934A`** aayega (aapke diye colours). Android ka Play app abhi bhi gold hai — use bhi copper karna hai ya waisa hi rehne dena hai? | Abhi Android waisa hi; iPhone naye colours par |

## A2. Design ke faisle — ✅ tay ho gaye 25 Sep 2026 (owner)

| # | Faisla | Owner ka jawab |
|---|---|---|
| D1 | Neeche ke 5th tab ka naam | **सेटिंग्स / Settings** (More nahi) |
| D2 | iPhone par login | **Google + Apple dono** |
| D3 | Hindi screen par bracket me English ("(Good Match)") | **Hatao** — dono app me |
| D4 | Muhurat ka samay sunrise→sunset | **Theek karo** — asli nakshatra/tithi samay aur sahi naam, Play aur iPhone dono |
| D5 | Naye Android build ka rang | **Copper**; abhi ka Play app gold rahega |
| D6 | Profile file me naam khali/null | **Chhod do**, import mat karo |
| D7 | Janm tithi Hindi ank me (१५-०८-१९९०) | **Apne aap 15-08-1990 me badlo** |
| D8 | Main rashi save nahi hoti (Play bug) | **Play me bhi theek karo**, agli Play release |
| D9 | GitHub ki 54 warnings (sab Gradle build tools ki, APK me nahi) | **Agli Play release ke saath** tools naye karo — ✅ 26 Sep 2026: Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20 + patched floors; **0 alerts khule**, 262 tests, lint, CI hare |
| D10 | Mockups v2 | **Manzoor** — screens banana shuru |

## B. Charan 0 se pehle — accounts (1–2 din)

| # | Kaam | Kaise |
|---|---|---|
| B1 | ✅ **Ho gaya** — `astroveda1`. **Naya Expo account** (expo.dev) sirf Revati ke liye banao, taaki builds kisi aur app ke saath na batein | Email aur password aap rakhna, mujhe sirf username batana |
| B2 | Terminal me `eas login` **aap khud** karna | Main command dunga |
| B3 | ✅ **Ho gaya** — `788G662STK`. Apple **Team ID** batao | developer.apple.com → Account → Membership details |
| B4 | App Store Connect me **naya app** banao: platform iOS, naam "Revati : Kundli & Panchang", primary language English, Bundle ID (A2), SKU `revati-ios` | Main screen-by-screen bataunga |
| B5 | ✅ **Ho gaya** — Key ID `5M6CRWLM5B`, file `~/secure-credentials/revati/` me. App Store Connect **API key** banao (Users and Access → Integrations → App Store Connect API, role "App Manager"). `.p8` file download karke `~/secure-credentials/revati/` me rakho. Key ID aur Issuer ID mujhe batao | Ye `eas submit` ke kaam aayegi |
| B6 | Jab EAS pehli baar Apple login maange, to **aap khud** login karna (2FA code aapke phone par aayega) | Certificate aur profile EAS khud banayega |
| B7 | Aadha ho gaya — iPhone 16. **iOS version abhi batana baaki hai** (Settings → General → About → Software Version) | Liquid Glass ke liye iOS 26 chahiye |
| B8 | ✅ TestFlight pehle se hai. Batao ki kaunsi Apple ID tester banegi. iPhone par **TestFlight** app install karo, aur batao ki kaunsi Apple ID email tester banegi | — |
| B9 | Expo slug ✅ `revati` kar diya. Expo par **Billing** page dekh kar batao ki free plan me har mahine kitni iOS builds milti hain | Builds ka hisaab rakhne ke liye |

## C. Charan 1 — design

| # | Kaam |
|---|---|
| C1 | Har screen ke mockup dekh kar **haan / badlav** batana (iPhone aur Android, Hindi aur English, light aur dark) |
| C2 | Tab bar aur glass look pasand hai, ye pakka karna |

## D. Charan 2 — calculation engine

| # | Kaam |
|---|---|
| D1 | Report dekhna jisme likha ho "0 differences". Isse pakka hoga ki naya engine Android app jaise hi jawab deta hai |

## E. Charan 4 se pehle — services (console ke kaam)

**Firebase** (production `astroveda-7126b` aur debug `revati-debug`, dono me):

| # | Kaam |
|---|---|
| E1 | **iOS app jodo** (Bundle ID A2; debug project me `.debug` laga kar). Dono `GoogleService-Info.plist` download karke folder me rakho |
| E2 | Firebase jo **iOS API key** banayega, use "iOS apps" + Bundle ID tak seemit karna. Android wali key ko haath nahi lagana |
| E3 | **App Check → App Attest** iOS app ke liye chalu karna |
| E4 | **Sign in with Apple** chalu karna: Apple par Services ID aur Key (.p8) banana, phir Firebase → Authentication → Apple me Services ID, Team ID, Key ID aur key file daalni. Main har kadam bataunga |
| E5 | Development build ka App Check **debug token** register karna. Token main nikaal kar dunga |

**AdMob** (agar A4 = haan):

| # | Kaam |
|---|---|
| E6 | AdMob me **iOS app** jodo (abhi store se link nahi hoga, wo launch ke baad) |
| E7 | Chaar **iOS ad units** banao: Banner, Interstitial, App open, Rewarded. Banner ka auto-refresh **45 second** rakhna. Chaaron IDs mujhe bhejo (ye password nahi hain) |

**App Store Connect (paise wala hissa):**

| # | Kaam |
|---|---|
| E8 | **Paid Apps Agreement** accept karo, aur **bank account** aur **tax forms** bharo. Iske bina test purchase bhi nahi chalega |
| E9 | **App Store Small Business Program** me apply karo. Isse Apple ka commission 30% se 15% ho jata hai |
| E10 | **Subscription** banao: group "Revati PRO", product ID `revati_pro_yearly`, avadhi 1 saal, keemat ₹199, naam aur vivaran English aur Hindi me. Main likh kar dunga |
| E11 | Test ke liye **Sandbox tester** account banao (koi nayi email) |
| E12 | Agar A6 = RevenueCat: RevenueCat par account banao aur app jodo. Main bataunga |

**Website aur policy:**

| # | Kaam |
|---|---|
| E13 | iPhone ke liye privacy policy ke naye hisse padh kar **haan** bolna (Apple sign-in, tracking/ATT, AI consent, RevenueCat). Uske baad main website par push karunga |

## F. Charan 5 — testing (aapke iPhone par)

| # | Kaam |
|---|---|
| F1 | TestFlight se app install karke `FEATURE_PARITY.md` ki har line check karna. Main ek chhoti checklist bana kar dunga |
| F2 | Hindi aur English, light aur dark, aur bade font (Settings → Display → Text Size) me dekhna |
| F3 | Sign-in teeno tarike se (Google, Apple, Email): backup, restore, aur account delete |
| F4 | Sandbox se PRO kharidna, phir "Restore" karna |
| F5 | **Normal WiFi** par ads dekhna (hotspot ads rok deta hai) aur tracking wala popup |
| F6 | Notifications aur home-screen widget |

## G. Charan 6 — App Store par bhejna

| # | Kaam |
|---|---|
| G1 | Listing ka text (subtitle, description, keywords) Hindi aur English me **approve** karna |
| G2 | Screenshots approve karna |
| G3 | Review ke liye contact: naam, phone, email |
| G4 | Age rating ke sawaalon ke jawab (main sujhav dunga) |
| G5 | Export compliance ka sawaal padh kar jawab pakka karna (app HTTPS aur local database encryption use karta hai) |
| G6 | Keemat (Free + in-app purchase) aur desh (A8) set karna |
| G7 | **"Submit for Review"** aap dabana |
| G8 | Approve hone ke baad release: khud dabana ya automatic |
| G9 | Launch ke baad: AdMob me App Store listing link karna, aur main website par App Store ka badge laga dunga |

## H. Pehle se chal rahe kaam (Android, iPhone se alag)

- BillDesk **Video KYC** aur Play merchant account
- Play ka **Licensing key**, jo `.env` me `PLAY_LICENSE_KEY` banegi
- Play par **2.3 / 202** upload, aur upload ke baad ads check karna
- Agli Android build me: FCM se sabko message, aur admin panel (scope aapse poochhna hai)
