# ARCHER AI — স্মার্ট অটো-মেমোরি আপডেট, প্রিমিয়াম ইমেজ ইঞ্জিন (Nano Banana Pro+) এবং মেইন জিমেইল লগইন প্ল্যান

এই আপডেটে অ্যাপের সমস্ত মেমোরি বাগ ও ছবি তৈরির সমস্যা দূর করে **স্বয়ংক্রিয় স্মার্ট মেমোরি ইঞ্জিন**, **Nano Banana Pro ও তার উপরের প্রিমিয়াম ইমেজ মডেল অটো-রাউটিং**, এবং **প্রিমিয়াম API Key ও মেইন জিমেইল (Google Account) লগইন সাপোর্ট** যুক্ত করা হবে।

## User Review & Critical Decisions

> [!IMPORTANT]
> **প্রশ্নোত্তর থেকে নিশ্চিতকৃত সিদ্ধান্তসমূহ (Confirmed Choices):**
> - **Confirmed Decision 1 (Smart Auto-Memory & Explicit Memory Sync)**: আপনি যখনই কোনো কিছু মনে রাখতে বলবেন (`"মনে রাখো..."`, `"মাথায় রেখো..."`) অথবা কথা বলার সময় কোনো দরকারি তথ্য (নাম, ঠিকানা, পছন্দ, রুটিন, কন্টাক্ট, প্রজেক্ট বা নিয়ম) আসবে, এআই নিজে বুঝে সাথে সাথে মেমোরি ট্যাবে (`Memory`) সেভ করবে এবং লাইভ সিস্টেম প্রম্পটে আপডেট করে নেবে।
> - **Confirmed Decision 2 (Premium API Key + Main Gmail Sign-In for Nano Banana Pro & Higher Models)**: আপনার প্রিমিয়াম **Gemini API Key** দিয়েই সরাসরি **Nano Banana Pro (`gemini-3-pro-image-preview` / `gemini-nano-banana-pro`)**, **Imagen 4 Ultra (`imagen-4.0-ultra-generate-001`)**, এবং **Gemini 2.5 Flash Image** কাজ করবে। এর পাশাপাশি মেইন সেটিংস ও ইমেজ পেইজে **মেইন জিমেইল আইডি (Google Account Sign-In)** দিয়ে লগইন করার অপশনও যুক্ত করা হবে, যাতে যে কারো প্রিমিয়াম জিমেইল বা পেইড API Key থাকলে সে সরাসরি সব প্রিমিয়াম সুবিধা ব্যবহার করতে পারে।

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **১০০% কার্যকর স্মার্ট মেমোরি ইঞ্জিন (`save_user_memory` টুল + রিয়েল-টাইম অটো-ডিটেকশন)**: ভয়েস বা টেক্সটে কথা বলার সময় এআই নিজে থেকেই গুরুত্বপূর্ণ তথ্য শনাক্ত করে ডাটাবেসে সেভ করবে এবং সাথে সাথে লাইভ মেমোরি প্রম্পটে যুক্ত করবে।
  2. **প্রিমিয়াম ইমেজ মডেল অটো-রাউটিং (Nano Banana Pro ও তার উপরের সব মডেল)**: সাধারণ বা ফ্রি মডেলের বদলে আপনার প্রিমিয়াম কী/অ্যাকাউন্ট থাকলে স্বয়ংক্রিয়ভাবে সর্বোচ্চ কোয়ালিটির প্রো মডেল (`Nano Banana Pro` / `Gemini 3 Pro Image` / `Imagen 4 Ultra` / `Gemini 2.5 Flash Image` / `Nano Banana 2.1`) ব্যবহার করে আসল ছবি তৈরি করবে।
  3. **প্রিমিয়াম API Key + মেইন জিমেইল (Google Account) লগইন**: ব্যবহারকারী চাইলে সরাসরি নিজের প্রিমিয়াম Gemini API Key বসাতে পারবেন অথবা **"🔐 মেইন জিমেইল আইডি দিয়ে লগইন করুন"** বাটনে ট্যাপ করে নিজের গুগল অ্যাকাউন্ট কানেক্ট করতে পারবেন।
- **Key Value**: কোনো দরকারি কথা আর ভুলে যাবে না, এবং প্রিমিয়াম সাবস্ক্রিপশন বা পেইড API Key-এর সর্বোচ্চ ক্ষমতার ইমেজ ও এআই মডেল সরাসরি অ্যাপের ভেতর পাওয়া যাবে।

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **স্মার্ট মেমোরি ফ্লো**:
     - আপনি মেইন স্ক্রিনে ভয়েসে বা টেক্সটে বললেন: *"মনে রেখো আমার মেইন প্রজেক্টের নাম আলফা"* বা যেকোনো ব্যক্তিগত তথ্য/নিয়ম বললেন।
     - সাথে সাথে Gemini Live-এর `save_user_memory` টুল এবং ব্যাকগ্রাউন্ড এআই এক্সট্রাক্টর সেটি `Memory` ট্যাবে সেভ করবে এবং ভয়েসে নিশ্চিত করবে।
  2. **প্রিমিয়াম রিয়েল ইমেজ স্টুডিও ও জিমেইল/API Key ফ্লো**:
     - ছবির কথা বললেই **Dedicated Real Image Studio Page** ওপেন হবে।
     - পেইজের উপরে বর্তমান অ্যাক্টিভ প্রিমিয়াম ইঞ্জিন ব্যাজ (`NANO BANANA PRO • IMAGEN 4 ULTRA • AUTO-ROUTING`) এবং কোন মডেল দিয়ে ছবিটি তৈরি হয়েছে তার নাম দেখাবে।
     - মেইন সেটিংস ও ইমেজ পেইজে **Premium API Key** এর পাশাপাশি **Main Gmail Sign-In Card** থাকবে, যেখানে লগইন করা জিমেইল আইডি ও প্রিমিয়াম স্ট্যাটাস দেখা যাবে।
     - ছবি বা কোড তৈরি শেষে আগের নিয়ম অনুযায়ী আপনার অনুমতি ছাড়া কোনো ফোল্ডারে সেভ হবে না—আগে জিজ্ঞেস করবে: *"এই কাজটা হয়ে গেছে—এটা কি 'archer' ফোল্ডারে সেভ করব?"*

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Tactical Cyber-Luxury Dark HUD (`#060913` ডিপ নেভি-ব্ল্যাক ব্যাকগ্রাউন্ড, `#00E5FF` সায়ান, `#00FF9C` নিওন গ্রিন ও `#F43F5E` রোজ অ্যাকসেন্ট)।
  - *Component Styling & Layout*: প্রিমিয়াম গ্লাসমরফিজম কার্ড, স্পষ্ট স্ট্যাটাস ব্যাজ, এবং ৪৮dp অ্যাক্সেসিবল বাটন।

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: প্রিমিয়াম API Key বনাম মেইন জিমেইল (Google Account) — উভয় অপশন রাখা**
  - *Chosen Approach*: Google AI Studio-র পেইড প্রজেক্টের ক্ষেত্রে **Premium Gemini API Key** সব প্রো মডেল (`Nano Banana Pro`, `Imagen 4 Ultra`, `Gemini 3 Pro Image`) সরাসরি আনলক করে। এর পাশাপাশি ব্যবহারকারীর সুবিধার জন্য **Android Credential Manager / Google Account Sign-In** যুক্ত করা হচ্ছে, যাতে ব্যবহারকারী নিজের মেইন জিমেইল অ্যাকাউন্ট লগইন করে রাখতে পারেন এবং প্রিমিয়াম প্রোফাইল ও ক্লাউড সুবিধা ব্যবহার করতে পারেন।
- **Decision 2: ডুয়াল-লেয়ার স্মার্ট মেমোরি (Live Tool + AI Turn Extractor)**
  - *Chosen Approach*: শুধু কিওয়ার্ড চেক করার বদলে দুটি স্তরে মেমোরি সেভ হবে: (১) মাল্টিমোডাল লাইভ সেশনে সরাসরি `save_user_memory` টুল কল, এবং (২) প্রতিটি ভয়েস ও টেক্সট কথোপকথন শেষে স্মার্ট তথ্য বিশ্লেষক যা দরকারি তথ্য অটো-সেভ করে এবং পরবর্তী প্রতিটি কমান্ডের প্রম্পটে সেই মেমোরি ইনজেক্ট করে।

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     ARCHER AI MAIN SCREEN & VOICE HUD                    │
└───────────────────┬──────────────────────────────────┬───────────────────┘
                    │                                  │
                    ▼                                  ▼
┌───────────────────────────────────────┐  ┌───────────────────────────────┐
│   DUAL-LAYER SMART MEMORY ENGINE      │  │  PREMIUM IMAGE STUDIO ENGINE  │
│  • Live Tool: save_user_memory        │  │  • Auto-Router Priority:      │
│  • Turn Analyzer: Auto Fact Extractor │  │    1. gemini-3-pro-image      │
│  • Room DB: MemoryNoteEntity          │  │    2. gemini-nano-banana-pro  │
│  • Live Prompt Sync: Injects all      │  │    3. imagen-4.0-ultra        │
│    saved memories into WebSocket      │  │    4. gemini-2.5-flash-image  │
│    & text turns in real time          │  │    5. gemini-nano-banana-2.1  │
└───────────────────────────────────────┘  └───────────────┬───────────────┘
                                                           │
                                                           ▼
                                           ┌───────────────────────────────┐
                                           │ PREMIUM AUTH & KEY MANAGER    │
                                           │  • Paid Gemini API Key        │
                                           │  • Main Gmail Sign-In         │
                                           │  • Ask-Before-Save Gate       │
                                           └───────────────────────────────┘
```

- **Data Model & State**:
  - `MemoryNoteEntity`: Room ডাটাবেসে সেভ হওয়া মেমোরি নোটগুলো রিয়েল-টাইম `StateFlow` হিসেবে মেমোরি ট্যাবে এবং `MultimodalLiveService`-এর সিস্টেম প্রম্পটে সিঙ্ক থাকবে।
  - `PremiumAccountState`: কানেক্টেড জিমেইল ইমেইল, ডিসপ্লে নাম, প্রিমিয়াম API Key স্ট্যাটাস এবং সর্বশেষ ব্যবহৃত প্রিমিয়াম ইমেজ মডেলের নাম ট্র্যাক করবে।
- **Interactive Component & State Mapping**:
  - মেইন স্ক্রিন বা লাইভ ভয়েস থেকে কোনো তথ্য মনে রাখতে বললে সাথে সাথে ডাটাবেসে ইনসার্ট হবে, মেমোরি কাউন্টার বাড়বে এবং লাইভ সেশনে পুশ হবে।
  - ইমেজ স্টুডিওতে ছবি তৈরির সময় প্রিমিয়াম মডেল চেইন (`:generateContent` ও Imagen `:predict` এন্ডপয়েন্ট) পর্যায়ক্রমে ট্রাই করবে এবং সফল মডেলের ব্যাজ প্রিভিউতে দেখাবে।
