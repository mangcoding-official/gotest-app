# Gotest

Aplikasi Android **Gotest** (bank soal, latihan, dan tryout) yang dibangun dari desain FigCraft
"BankSoal-new (Copy)", page **📱 Mobile App**. Ditulis dengan Kotlin dan Jetpack Compose (Material 3).
Backend belum ada, jadi semua data berasal dari Fake repository berisi data dummy.

## Menjalankan

- Butuh Android Studio versi stabil terbaru. Proyek ini memakai AGP 9.4.1; Android Studio 2024.3 tidak
  bisa membukanya. Butuh juga JDK 17+ dan Android SDK 37.
- Build dan pemeriksaan: `./gradlew assembleDebug ktlintCheck detekt lintDebug`
- **Login dummy:** email apa pun yang valid dengan password minimal 8 karakter, atau tombol
  "Masuk dengan Google".
- **Kode Main Bareng dummy:**

  | Kode | Hasil |
  |---|---|
  | `KTR4QZ` | Room ditemukan, sedang menunggu pemain |
  | `MNB7RT` | Permainan sudah dimulai |
  | `FULL10` | Permainan penuh |
  | `BATAL1` | Permainan sudah batal |
  | Kode lain | "Kode tidak ditemukan" |

- **Katalog design system (khusus build debug):**
  `adb shell am start -n com.gotest.app/.catalog.DesignSystemCatalogActivity`

## Stack

| Bagian | Pilihan |
|---|---|
| Bahasa / UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09.00), Material 3 |
| Arsitektur | MVVM + UDF, clean architecture ringan (ui / domain / data), package-by-feature |
| DI | Hilt (KSP) |
| Navigasi | Navigation Compose, type-safe destinations (`kotlinx-serialization`) |
| Gambar | Coil 3 |
| Kamera / QR | CameraX (`camera-compose`) untuk Ambil Foto, ZXing core untuk kode QRIS |
| Kualitas kode | ktlint (`ktlint_official`) + compose-rules, detekt + compose-rules, Android Lint |
| SDK | minSdk 24, targetSdk/compileSdk 37; `desugar_jdk_libs` untuk `java.time` di API 24–25 |

## Struktur

```
app/src/main/java/com/gotest/app/
  core/
    designsystem/theme/      Color, Type, Shape, Dimens, Theme (GotestTheme)
    designsystem/component/  komponen reusable (lihat di bawah)
    designsystem/icon/       GotestIcons (ikon Vuesax dari desain, vector drawable)
    model/                   model domain bersama
    domain/repository/       interface repository bersama
    data/fake/               Fake repository + data dummy; data/di/DataModule.kt (binding Hilt)
    ui/                      GotestFormat (tanggal/rupiah id-ID), ObserveAsEvents, examChipTone, DrawableNameMapper
  navigation/                GotestDestinations (kunci navigasi), GotestApp (shell + bottom bar), GotestNavHost
  feature/<fitur>/
    navigation/              fun NavGraphBuilder.<fitur>Screens(navController)
    domain/ data/ data/di/   repository khusus fitur (bila perlu)
    ui/<layar>/              XxxUiState (+ UiEvent/Effect), XxxViewModel, XxxScreen (XxxRoute + XxxScreen + preview)
```

Fitur yang ada: `auth`, `home` (Beranda, Pencarian, Notifikasi), `studypackage` (Paket), `exam` (Ujian,
Hasil, Pembahasan), `history`, `leaderboard`, `account` (Profil, Akun, Pengaturan, Langganan, Bantuan),
`upgrade`, `assistant` (Asisten AI), dan `multiplayer` (Main Bareng). Totalnya 49 destinasi.

Aturan layar:
- `XxxRoute` mengambil ViewModel dengan `hiltViewModel()` dan `collectAsStateWithLifecycle()`, lalu
  meneruskan lambda navigasi.
- `XxxScreen` stateless: hanya menerima UiState dan lambda event.
- Setiap layar data punya state Loading / Error / Empty / Success dan preview untuk setiap state.

## Mengganti data dummy dengan API

UI dan ViewModel hanya bergantung pada interface repository, jadi yang perlu diganti cukup binding Hilt:

1. Implementasikan `XxxRepository` versi remote (Retrofit/Ktor; library belum ditambahkan).
2. Ganti `@Binds` di `core/data/di/DataModule.kt` (Auth, User, StudyPackage, Exam, Leaderboard,
   Subscription) dan di modul per fitur:
   - `feature/home/data/di` (Notification, Search)
   - `feature/studypackage/data/di` (SavedPackage)
   - `feature/history/data/di` (History, HistoryExporter)
   - `feature/account/data/di` (Settings, Help, AccountData, ProfilePhotoProcessor)
   - `feature/upgrade/data/di` (Promo)
   - `feature/assistant/data/di` (Assistant, AttachmentReader)
   - `feature/multiplayer/data/di`
3. Gambar dummy dimuat lewat `drawableUri("nama")` dan `DrawableNameMapper`. Gambar dari server cukup berupa
   URL http(s). Setelah data dummy dihapus, mapper boleh ikut dihapus.

## Design system

**Token**
- **Warna:** 60 token semantik: warna dari variable `🎨/color/*`, ditambah token medali/podium dan
  `borderStrong`. Hanya light mode.
- **Tipografi:** Inter (Regular/Medium/SemiBold) dan BBH Bogle, dipetakan ke role Material. Ada tambahan
  `displayExtraSmall`, `buttonLarge/Small`, `bodyExtraLarge`, dan `labelCompact`.
- **Bentuk:** radius 4/8/12/16/20/24/full.
- **Spacing:** 0/2/4/8/12/16/24/32/40, gutter layar 20.
- **Ukuran komponen:** di `GotestSizes`.

**Komponen reusable** (`core/designsystem/component`)
- `GotestButton` (Filled/Plain/Text; L/M/S)
- `GotestTextField`, `GotestPasswordField`
- `GotestIconButton`, `GotestChip` (7 tone), `GotestFilterPill`, `GotestSegmentedControl`, `GotestToggle`
- `GotestTopAppBar` (Centered/Leading) + `GotestBackButton`
- `GotestBottomBar` (pill + FAB asisten)
- `GotestCard`, `GotestDialog`/`GotestDialogContent`/`GotestIconBadge`, `GotestBottomSheet`
- `GotestLoadingView`/`GotestEmptyView`/`GotestErrorView`
- `GotestProgressBar`, `GotestTrackedProgress`, `GotestAvatar`, `GotestPackageCard`, `GotestActivityRow`,
  `GotestDivider`
- Komponen soal: `GotestAnswerOption`, `GotestCheckOption`, `GotestStatementOption`, `GotestQuestionCell`,
  `GotestStimulusCard`, `GotestQuestionOptions`

Komponen khusus fitur ada di `feature/<fitur>/ui/components`. Contohnya Menu Row (account) dan
MB/Player Row, Code Box, Alert, Rank Row, Duel Result Banner (multiplayer). Ukuran yang tidak punya token
desain disimpan sebagai konstanta bernama di objek `XxxDimens` milik fiturnya.

## TODO

**Perlu keputusan atau aset**
- [ ] **Dark theme:** desain hanya punya palet light. Palet dark perlu dirancang dulu.
- [ ] **Ikon yang tidak ada di file desain:** jam (timer ujian dan Main Bareng), target, mahkota, dan
  panah tren. Sementara diganti `Calendar`, `Award`, `ArrowUp/Down`; lihat `TODO(design)` di kode.
- [ ] **Teks yang ditulis sendiri (bukan dari desain), perlu dicek copywriter:**
  - validasi form, state error/empty, snackbar, teks share, dan dialog Keluar
  - isi FAQ dan topik bantuan "Paket" dan "Pembayaran"
  - isi file ekspor Riwayat (judul dan kolom PDF/CSV), seed percakapan Asisten, dan balasan Asisten
    untuk lampiran
  - "Monitoring update data sistem secara realtime." (subjudul Aktivitas Terkini) disalin dari desain
    tapi terdengar seperti teks admin
- [ ] **Desain yang tidak konsisten:** gaya judul app bar (SemiBold 16/16 vs Medium 16/24) dan angka mockup
  yang saling bertentangan (mis. akurasi 100% di Beranda vs 86% di Profil). Aplikasi memakai data dummy
  yang konsisten. Contoh lain: promo Semangat Merdeka mencoret harga Rp200.000, padahal paket Premium
  Tahunan di Upgrade Akun memang Rp100.000.

**Belum diimplementasikan**
- [ ] Deep link `https://gotest.id/main/{kode}` untuk mengisi kode Main Bareng otomatis. Perlu
  intent-filter, `deepLinks`, dan parameter kode.

**Untuk API nanti**
- [ ] Sisa waktu tryout perlu disimpan (`saveRemainingTime`). Saat ini tryout yang dilanjutkan memulai
  timer dari awal sesi.
- [ ] "Ulangi Latihan" butuh endpoint untuk memulai ulang attempt.
- [ ] Field model yang dibutuhkan UI:
  - `LeaderboardEntry.completedOn`
  - waktu "Diperbarui" untuk peringkat per paket
  - `StudyPackage.categoryId` dan jumlah topik
  - id tryout seasonal yang asli
  - akurasi per attempt untuk urutan Riwayat (sekarang dihitung di `FakeHistoryRepository`)
- [ ] Endpoint untuk Paket Tersimpan, riwayat percakapan Asisten, upload lampiran Asisten, dan promo.
- [ ] Pembayaran QRIS/E-Wallet: payload QRIS dan tautan pembayaran e-wallet harus datang dari payment
  gateway. Sekarang QR dibuat dari payload dummy dan "Buka <wallet>" hanya membuka aplikasinya lewat nama
  paket. "Ganti metode pembayaran" juga perlu membatalkan order lama.
- [ ] Foto profil dari galeri baru berupa `content://` URI sementara, dan foto dari kamera berupa file di
  `cacheDir/profile_photo/`; keduanya perlu di-upload ke server.
- [ ] URL Kebijakan Privasi di Data Pribadi (`account_personal_policy_url`) masih tebakan:
  `https://gotest.id/kebijakan-privasi`.

**Teknis**
- [ ] Unit test ViewModel dan aturan domain (mis. `ExamRules`, `GameRules`, `CredentialValidator`) belum
  ditulis. Fungsinya sudah murni sehingga mudah dites.
- [ ] Konfigurasi release: `isMinifyEnabled = false` dan signing belum diatur.
- [ ] Animasi: hanya ada yang tersirat di desain (toggle, waveform voice, label tab, countdown).
  Transisi antar layar memakai default Navigation.
- Lisensi font (OFL) ada di `licenses/fonts/`.
