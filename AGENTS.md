# Petunjuk agen untuk repo gotest-app (aplikasi Android Gotest)

Berlaku untuk semua AI (Claude, Cursor, Codex, atau lainnya) yang bekerja di repo ini.

## Satu pintu: aturan, tugas, dan keputusan ada di repo web

Repo ini hanya berisi kode aplikasi Android. Semua yang lain ada di repo web `bank-soal-tryout`
(`https://github.com/mangcoding-official/bank-soal-tryout`), branch `development`:

- `AGENTS.md` di akar repo web: aturan agen, alur kerja tim, dan aturan produk yang dikunci. Aturan itu
  berlaku juga di sini, kecuali bagian yang khusus Laravel, PHP, atau Vite.
- `docs/kerja/`: `STATUS.md` (tugas aktif, baris "Berikutnya:", jawaban dan catatan pemeriksaan Ardy),
  `ALUR-KERJA.md` (aturan kerja tim; bagian 7c khusus repo ini), `tugas/T-xxx-*.md`, `catatan/<nama>.md`,
  dan `KODE.md` (arti kode seperti DEC, T, K, A1c).
- `docs/gotest/`: aturan produk. Kontrak API ada di `docs/gotest/teknis/API-CONTRACTS.md` dan
  `docs/kerja/api/preflight-api-android.md`.
- Halaman web (`resources/js/Pages/`) adalah acuan teks dan alur setiap layar aplikasi.

Jangan menyalin aturan, tugas, atau keputusan ke repo ini, dan jangan memakai salinan lama. Selalu baca
dari `development` repo web yang terbaru.

## Langkah wajib di awal sesi

Selesaikan berurutan sebelum membaca berkas lain atau mengerjakan permintaan apa pun.

1. **Tanyakan, lalu tunggu jawabannya:** "Kamu siapa: Ardy, Samsul, atau Deni?" dan "Tugas nomor berapa
   (T-xxx)?". Jangan menebak dari nama akun git, nama laptop, memori, atau catatan sesi lain. Ulangi
   jawabannya untuk dikonfirmasi: "Saya bekerja untuk <nama>, tugas T-xxx."
2. **Temukan repo web di laptop ini:** `git config gotest.repo-web` (dijalankan di repo ini). Bila kosong,
   tanyakan letak folder clone `bank-soal-tryout`, pastikan folder itu berisi `docs/kerja/STATUS.md`
   setelah `git fetch`, lalu setel setelah disetujui: `git config gotest.repo-web <path>`. Bila repo web
   belum di-clone, minta orangnya meng-clone dulu.
3. **Tarik repo web terbaru:** `git -C <repo-web> fetch origin`.
   - Repo web ada di `development` dan bersih: `git -C <repo-web> pull --ff-only`.
   - Repo web ada di branch lain atau punya perubahan yang belum di-commit: jangan pindah branch dan jangan
     menyentuh perubahannya. Baca dari `origin/development` dengan
     `git -C <repo-web> show origin/development:<path>`.
4. **Siapkan repo ini:** `git fetch origin`, lalu `git status`.
   - Branch tugas `<nama>/T-xxx-judul` sudah ada: `git switch` ke sana, lalu `git merge origin/main`.
   - Belum ada: `git switch -c <nama>/T-xxx-judul origin/main`.
   - Jangan menghapus atau menimpa perubahan yang belum di-commit; tanyakan dulu.
5. **Penjaga push repo ini:** `git config core.hooksPath` harus `.githooks`, dan `git config gotest.nama`
   harus nama orangnya dengan huruf kecil. Bila belum, tunjukkan perintahnya dan jalankan setelah disetujui:
   `git config core.hooksPath .githooks` dan `git config gotest.nama <nama>`. Bila `gotest.nama` berisi
   nama orang lain, jangan mengubahnya; beri tahu dan tunggu. Khusus Ardy: `gotest.boleh-gabung` = `true`.
6. **Baca dari repo web (`development` terbaru):** `AGENTS.md` bagian "Team Workflow",
   `docs/kerja/STATUS.md`, `docs/kerja/ALUR-KERJA.md` (terutama bagian 6 dan 7c), berkas tugas
   `docs/kerja/tugas/T-xxx-*.md`, dan `docs/kerja/catatan/<nama>.md`.

Setelah itu laporkan singkat hasil langkah 2 sampai 5, bacakan baris "Berikutnya:" tugas itu dari
`STATUS.md` apa adanya, jelaskan tugasnya dengan bahasa sederhana beserta rencana kerjamu, lalu tunggu.
Bila tidak ada tugas Siap atau Dikerjakan atas nama orang itu, katakan itu dan sarankan bertanya ke Ardy.

## Selama bekerja

- **Kode aplikasi** hanya di repo ini, di branch `<nama>/T-xxx-judul` yang dibuat dari `origin/main`.
  Push hanya ke branch sendiri. Jangan merge ke `main` atau branch orang lain, dan jangan menekan tombol
  Merge di GitHub; yang menggabungkan hanya Ardy. Jangan memakai `--no-verify`.
- **Catatan serah terima, catatan pribadi, dan pertanyaan** ditulis di repo web, di branch dengan nama yang
  sama (`<nama>/T-xxx-judul`, dibuat dari `origin/development`), hanya pada berkas tugas dan
  `docs/kerja/catatan/<nama>.md`. Sebutkan commit `gotest-app` yang terkait di catatan itu.
- **Butuh perubahan server atau API:** tulis di "Perlu dari BE" berkas tugas. Jangan menebak bentuk JSON
  dan jangan mengubah kontrak sendiri; kontrak hanya berubah lewat Ardy.
- **Keputusan:** pilihan teknis diambil sendiri dan dicatat di "Keputusan teknis". Keputusan bisnis dan
  alur (apa yang dilihat pengguna, teks, alur, hak akses, kontrak) ditulis di "Perlu keputusan Ardy" dengan
  pilihan dan usulan. Lihat `ALUR-KERJA.md` bagian 6 di repo web.
- **Data palsu** hanya untuk varian debug. Build release ditolak selama modul di `src/main` masih memakai
  kelas `Fake…` (tugas Gradle `checkNoFakeData`).
- **Fitur yang belum ada di web** disembunyikan lewat `core/config/AppFeatures.kt`, bukan dihapus.

## Sebelum minta diperiksa dan sebelum sesi berakhir

1. Jalankan `./gradlew assembleDebug ktlintCheck detekt lintDebug testDebugUnitTest`, lalu cek di emulator.
   Tulis hasilnya di berkas tugas.
2. Isi "Catatan serah terima" di berkas tugas dan catatan sesi di `docs/kerja/catatan/<nama>.md` (repo web).
3. Commit dan push kedua branch: branch kode di repo ini dan branch catatan di repo web.
4. Saat tugas siap diperiksa, ubah statusnya menjadi Diperiksa di berkas tugas dan kabari Ardy di grup tim.
