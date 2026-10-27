# ARCHER AI — Multimodal Live API ইমপ্লিমেন্টেশন প্ল্যান

আপনার নির্দেশনা অনুযায়ী **ARCHER AI**-তে পূর্ণাঙ্গ **Multimodal Live API** (Real-time Bidirectional Voice + Live Camera Vision + Live Screen Sharing + Live Phone Tool Execution) এবং **Live API Key & Endpoint এক্সপোর্ট/কনফিগারেশন প্যানেল** যুক্ত করা হবে।

---

## ১. অ্যাপের নতুন ফিচার ও মূল সুবিধাসমূহ

### ক. মাল্টিমোডাল লাইভ সেশন (Voice + Camera + Screen একসাথে)
- **রিয়েল-টাইম টু-ওয়ে ভয়েস (Bidi Audio & Voice Activity Detection):** বারবার বাটনে চাপ না দিয়েই সরাসরি কথা বলা যাবে। আপনি কথা বললে এজেন্ট সাথে সাথে শুনবে এবং কথা বলার মাঝখানে কথা বললে (Barge-in / Interruption) নিজে থেমে আপনার নতুন নির্দেশ শুনবে।
- **লাইভ ক্যামেরা স্ট্রিমিং (Live Camera Frames):** সামনের বা পেছনের ক্যামেরা চালু রেখে যেকোনো বস্তু, লেখা বা পরিবেশ দেখালে মাল্টিমোডাল লাইভ এপিআই রিয়েল-টাইম ফ্রেম বিশ্লেষণ করে উত্তর দেবে।
- **লাইভ স্ক্রিন শেয়ারিং (Live Screen Capture Stream):** ফোনের স্ক্রিনে যা চলছে তা লাইভ ফ্রেম হিসেবে এআই দেখতে পাবে এবং স্ক্রিনের যেকোনো অ্যাপ বা সেটিংস দেখে গাইড করবে বা অ্যাকশন নেবে।

### খ. Multimodal Live API এক্সপোর্ট ও কাস্টম কানেকশন হাব (API Inspector & Custom Key)
- **Live API Key ও কনফিগ এক্সপোর্ট (`বের করে দেওয়া`):** অ্যাপের ভেতর থেকে প্রফেশনাল **Multimodal Live API Hub** খোলা যাবে, যেখান থেকে:
  - সক্রিয় **Project ID, Application ID, Built-in Firebase AI Key স্ট্যাটাস এবং WebSocket Live Endpoint (`wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent`)** দেখা ও এক ক্লিকে কপি/এক্সপোর্ট করা যাবে।
  - রেডিমেড **WebSocket JSON Setup Payload** ও **Live Tool Declarations** কপি করার সুবিধা থাকবে।
- **ডুয়াল-ইঞ্জিন সুইচ (Dual-Engine Live Mode):**
  1. **Built-in Cloud Multimodal Streaming (ডিফল্ট):** কোনো আলাদা কি ছাড়াই অ্যাপের বিল্ট-ইন Firebase AI Logic (`gemini-2.5-flash` লাইভ স্ট্রিমিং) দিয়ে তাৎক্ষণিক কাজ করবে।
  2. **Direct Gemini Multimodal Live WebSocket (`BidiGenerateContent`):** আপনি চাইলে নিজের Gemini API Key বসিয়ে সরাসরি লো-লেটেন্সি WebSocket লাইভ স্ট্রিমিং চালাতে পারবেন।

### গ. লাইভ কথার মাঝেই ফোনের অ্যাকশন ও সেটিংস কন্ট্রোল (Real-Time Live Function Calling)
- লাইভ সেশন চলাকালীন আপনি মুখে বললেই (যেমন: *"টর্চলাইট জ্বালাও"*, *"ভলিউম ফুল করো"*, *"হোয়াটসঅ্যাপ খোলো"*, *"স্ক্রিনে ওপরে স্ক্রল করো"*, *"ব্লুটুথ সেটিংস খোলো"*) এজেন্ট কথা বলার পাশাপাশি সাথে সাথে ফোনের সেই কাজটি সম্পন্ন করবে এবং লাইভ টুল ব্যানারে দেখিয়ে দেবে।

---

## ২. ইউজার ইন্টারফেস ও স্ক্রিন ডিজাইন

- **Live API Studio Bar (হোম ও ব্রিজ স্ক্রিনে):**
  - এক ট্যাপে **"START MULTIMODAL LIVE"** চালু করার বাটন এবং পাশে **"LIVE API KEY & CONFIG"** বাটন।
- **Multimodal Live HUD ওভারলে:**
  - লাইভ অডিও ওয়েভফর্ম অ্যানিমেশন, সক্রিয় ইনপুট ব্যাজ (`MIC LIVE`, `CAM STREAM`, `SCREEN LIVE`, `TOOLS ARMED`), ফ্রেম রেট ও লেটেন্সি মিটার এবং তাৎক্ষণিক মিউট/ক্যামেরা/স্ক্রিন টগল।
- **Live API Key & WebSocket এক্সপোর্ট শিট:**
  - অ্যাপের বর্তমান কনফিগারেশন, WebSocket URL, Model ID, Custom Gemini Live API Key ইনপুট ফিল্ড এবং এক ক্লিকে JSON/Config কপি বাটন।
