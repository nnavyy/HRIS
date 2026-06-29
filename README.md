# HRIS - Aplikasi Manajemen SDM Digital

Aplikasi HRIS (Human Resource Information System) adalah platform digital terpadu yang dirancang untuk mempermudah, mengotomatisasi, dan mendigitalkan seluruh proses manajemen Sumber Daya Manusia di perusahaan. 

Aplikasi ini tidak hanya sekadar alat pencatat absen, melainkan sebuah ekosistem lengkap yang menghubungkan Karyawan, Manajer, HRD, hingga tim Keuangan (Finance) dalam satu alur kerja (alur proses) yang transparan dan efisien.

---

## 🎯 Kegunaan Utama
1. **Mendisiplinkan Kehadiran**: Mencegah titip absen dengan sistem validasi GPS (berdasarkan lokasi kantor) dan foto *selfie* langsung dari kamera.
2. **Otomatisasi Penilaian Kinerja (KPI)**: Kinerja dinilai secara objektif menggunakan data absensi (keterlambatan mengurangi skor), *Peer Review* (penilaian antar rekan kerja dengan sistem bintang 1-5), dan dirangkum secara cerdas oleh AI.
3. **Digitalisasi Dokumen**: Mengubah proses tanda tangan kontrak kerja yang tadinya di atas kertas menjadi tanda tangan digital (*e-signature*) langsung di layar HP.
4. **Transparansi Gaji & Cuti**: Karyawan dapat melihat sisa jatah cuti, memantau status persetujuan cuti, serta mengunduh slip gaji bulanan mereka sendiri tanpa perlu terus-menerus bertanya ke HRD.

---

## 👥 Alur Kerja Berdasarkan Peran (Role)

Aplikasi ini memiliki 5 jenis pengguna dengan hak akses dan *dashboard* yang berbeda-beda agar setiap orang hanya fokus pada tugasnya masing-masing.

### 1. Karyawan (Employee)
Karyawan adalah pengguna utama aplikasi untuk keperluan *self-service*.
- **Alur Absensi**: Setiap pagi dan sore, karyawan membuka aplikasi untuk melakukan *Check-In* dan *Check-Out*. Sistem akan mengecek apakah lokasi HP karyawan berada di dalam radius kantor. Karyawan juga diwajibkan melakukan *selfie*.
- **Alur Cuti**: Karyawan dapat mengajukan cuti dengan memilih tanggal. Sistem otomatis mengecek apakah sisa cuti masih ada dan apakah pengajuan dilakukan tidak mendadak (mematuhi aturan H-sekian). Pengajuan cuti ini akan masuk ke Manajer untuk disetujui.
- **Alur Penilaian (Peer Review)**: Karyawan dapat memberikan penilaian rekan kerjanya menggunakan sistem 1-5 Bintang untuk menilai performa, kualitas, kontribusi, dan etika.
- **Alur Tanda Tangan**: Karyawan baru dapat membaca draf kontrak kerja dan langsung menandatanganinya di layar HP.
- **Alur Slip Gaji**: Karyawan dapat melihat slip gaji yang sudah di-generate oleh tim HR dan disetujui oleh Finance setiap bulannya.

### 2. Manajer (Manager)
Manajer bertugas mengawasi tim di bawah departemennya.
- **Alur Persetujuan Cuti**: Ketika karyawan di timnya mengajukan cuti, Manajer akan mendapat notifikasi. Manajer berhak menekan tombol *Setujui* atau *Tolak*. 
- **Pantauan Tim**: Manajer memiliki menu khusus untuk memantau siapa saja di timnya yang hadir, absen, atau sedang cuti pada hari ini.

### 3. HRD (HR / Admin)
HRD adalah penggerak utama administrasi perusahaan.
- **Alur Data Master**: HRD membuat akun untuk karyawan baru, mendaftarkan wajah karyawan, dan mengatur jadwal kerja karyawan (misal: Shift Pagi, Shift Malam).
- **Alur Penilaian AI (AI Review)**: Di akhir bulan, HRD dapat menekan tombol **"Generate AI Review"**. Aplikasi akan mengirimkan data absensi, data keterlambatan, dan hasil *peer review* ke kecerdasan buatan (AI Groq/Llama 3). AI akan membuatkan laporan evaluasi kinerja secara otomatis beserta saran perbaikan untuk karyawan tersebut!
- **Alur Penggajian (Payroll)**: HRD menekan tombol *Generate Payroll* di akhir bulan. Sistem akan menghitung gaji pokok, memotong gaji karena keterlambatan (otomatis terhubung ke absensi), dan menghitung uang lembur. Draft slip gaji ini kemudian dikirim ke departemen Finance.
- **Alur Kontrak**: HRD membuat draf kontrak untuk karyawan dan memantau status tanda tangannya.

### 4. Keuangan (Finance)
Departemen Keuangan bertugas memastikan tidak ada kesalahan angka sebelum uang dicairkan.
- **Alur Validasi Gaji**: Finance akan menerima *draft* gaji yang dibuat HRD. Setelah di-cek dan dirasa sesuai, Finance akan menekan tombol *Approve*. Barulah slip gaji tersebut muncul di aplikasi Karyawan.

### 5. Super Admin
Super Admin adalah pemilik sistem (biasanya tim IT perusahaan) yang memiliki kontrol penuh.
- **Pengaturan Inti**: Super Admin mengatur titik lokasi GPS kantor pusat dan cabang, radius toleransi absen (misal: 50 meter), mengatur hak akses pengguna (siapa yang jadi HR, siapa yang jadi Manager), dan menyetel *API Key* untuk sistem AI.
- **Audit**: Super Admin dapat melihat jejak aktivitas (siapa mengubah apa, jam berapa).

---

## 🔄 Bagaimana Fitur-fiturnya Saling Terhubung?

Aplikasi ini tidak bekerja sendiri-sendiri, melainkan saling menunjang:

1. **Jadwal Kerja ➔ Absensi ➔ Pemotongan Gaji**
   HR mengatur jadwal kerja `Masuk jam 08:00`. Jika karyawan absen jam `08:30`, sistem mencatat "Telat 30 Menit". Saat HR membuat Slip Gaji di akhir bulan, gaji otomatis dipotong berdasarkan total menit keterlambatan tersebut.

2. **Absensi ➔ KPI Otomatis ➔ AI Review**
   Setiap kali karyawan telat, skor *Kedisiplinan* (KPI) mereka akan otomatis berkurang oleh sistem di latar belakang. Skor yang berkurang ini, digabungkan dengan ulasan Bintang dari rekan kerja, akan dikirim ke **AI** untuk dicerna. AI kemudian akan memberikan kesimpulan evaluasi bulanan yang bisa dibaca oleh Manajer dan HR.

3. **Kuota Cuti ➔ Pengajuan Cuti ➔ Penolakan Otomatis**
   Sistem mengatur batas pengajuan cuti minimal H-3. Jika karyawan memaksakan mengajukan cuti untuk besok pagi, sistem akan **menolak (Auto-Reject)** pengajuan tersebut secara otomatis tanpa perlu menunggu keputusan Manajer.

---

*Aplikasi ini memastikan proses administrasi SDM yang tadinya memakan waktu berhari-hari (karena rekap manual Excel dan kertas) kini dapat diselesaikan hanya dalam beberapa klik saja dari genggaman tangan.*
