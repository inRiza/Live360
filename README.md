# Nimons360

Aplikasi mobile Android untuk membantu pengguna mengelola dan memantau family. Fitur utama meliputi autentikasi, manajemen family, dan pemantauan anggota family.

Spesifikasi aplikasi mengacu pada dokumen tugas besar.

---

## Deskripsi

Nimons360 adalah aplikasi berbasis Android yang memungkinkan pengguna untuk:

- Login dan mengakses sistem dengan token
- Melihat daftar family yang diikuti (My Families)
- Menemukan family baru (Discover Families)
- Melihat detail family dan anggota
- Bergabung atau keluar dari family

---

## Library yang Digunakan

- Retrofit (API communication)
- Glide (image loading)
- Hilt (dependency injection)
- RecyclerView (list rendering)
- ViewBinding
- MapLibre

---

## Screenshot

1. Home

![Home](screenshots/1home.jpeg)

2. Family Detail

![Family Detail](screenshots/2famdetail.jpeg)

3. Profile

![Profile](screenshots/3profile.jpeg)

4. Map

![Map](screenshots/4map.jpeg)

5. Detail User (Map Bottom Sheet)

![Detail User](screenshots/5detailuser.jpeg)

6. Families List

![Families List](screenshots/6famlist.jpeg)

7. Join Family

![Join Family](screenshots/7joinfam.jpeg)

8. Create Family

![Create Family](screenshots/8createfam.jpeg)

9. My Families

![My Families](screenshots/9myfam.jpeg)

10. Network Sensing

![Network Sensing](screenshots/10networksensing.jpeg)

11. Sign Out Dialog

![Sign Out Dialog](screenshots/11signout.jpeg)

12. Leave Family Dialog
    
![Leave Family Dialog](screenshots/12leavefam.jpeg)

---

## Cara Menjalankan

1. Install Android Studio
2. Pastikan SDK:
  - Minimum SDK: Android 11 (API 30)
  - Compile SDK: Android 15
3. Pastikan Kotlin sudah terpasang
4. Clone repository
5. Buka project di Android Studio
6. Sync Gradle
7. Jalankan emulator atau hubungkan device
8. Klik Run

---

## Kontribusi


| NIM      | Nama                    | Tugas                                                                                                          |
| -------- | ----------------------- | -------------------------------------------------------------------------------------------------------------- |
| 13523144 | Muhammad Nazih Najmudin | Login, Families (Create, Leave & Join, Landing & Pinned & Filter)                                              |
| 13523158 | Lukas Raja Agripa       | OpenAPI (.yaml, runner), Map (Internet Status & Mark Location)                                                 |
| 13523164 | Muhammad Rizain Firdaus | Profile & Edit, Logout, Network Sensing, Header & Bottom Bar (Navigation), Home (MyFamily & Discover Families) |


---

## Catatan

- Aplikasi menggunakan backend yang telah disediakan
- Token digunakan untuk autentikasi setiap request

---

## Bonus
- Search Family
- Internet Status
- Mark Favorite Location

