# Immich Auto Downloader

Android app dong bo media mot chieu tu Immich server ve may Android de Google Photos co the tu dong backup.

## Muc tieu

App nay tap trung vao luong:

`Immich Server -> Android public media folder -> Google Photos backup`

App khong xoa file tren Android va co co che tranh tai trung dua tren `asset_id` da luu local.

## Tinh nang hien co

- Nhap `Immich Server URL` va `API Key`
- Test ket noi toi Immich API
- Dong bo media tu Immich ve Android
- Luu file vao:
  - `Pictures/ImmichSync`
  - `DCIM/Camera`
- Dung `Room` de track asset da tai
- Ghi log thanh cong / that bai
- Ho tro `Sync Now` va `Retry`
- Tu dong sync nen voi chu ky co dinh 1 phut
- Ho tro dieu kien:
  - `Wi-Fi only`
  - `Charge only`
  - `Battery not low`
- Xem va copy `Device info JSON`

## Kien truc chinh

- `Jetpack Compose` cho UI
- `Retrofit + OkHttp` cho Immich API
- `Room` cho local database
- `DataStore` cho settings
- `WorkManager` cho background sync
- `MediaStore` de luu media vao public storage

## API dang dung

- `POST /api/search/metadata`
- `GET /api/assets/{assetId}/original`

Download file goc duoc danh dau `@Streaming` de tranh buffer ca file lon vao RAM.

## Cac man hinh

- `Home`
  - tong so asset
  - so file da tai
  - so file pending
  - lan sync cuoi
  - nut `Sync Now`
- `Settings`
  - server URL
  - API key
  - thu muc tai
  - Wi-Fi only
  - Charge only
  - Auto sync
  - Device info JSON + copy
- `Logs`
  - danh sach log
  - nut `Retry`

## Build

Yeu cau:

- Android Studio moi
- Android SDK da cai
- Java 17+

Build debug APK:

```bash
./gradlew :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Cai dat va su dung

1. Mo project trong Android Studio
2. Build va cai APK len thiet bi Android
3. Mo app
4. Nhap `Immich Server URL`
5. Nhap `API Key`
6. Bam `Test Connection`
7. Chon thu muc tai mong muon
8. Bam `Save`
9. Bam `Sync Now`
10. Bat backup thu muc tuong ung trong Google Photos

## Luu y ky thuat

- Android khong cho `PeriodicWorkRequest` duoi 15 phut, vi vay app dung `OneTimeWorkRequest` tu reschedule de dat chu ky 1 phut.
- Doze mode va background limits cua Android van co the lam tre lich chay thuc te.
- App hien tai dang lay toi da `1000` asset moi lan sync.
- API key hien dang luu trong `DataStore`, chua ma hoa.

## Cau truc project

```text
app/
  src/main/java/com/immich/autodownloader/
    data/
    device/
    sync/
    ui/
gradle/
```

## Huong mo rong tiep

- Phan trang day du cho thu vien Immich lon
- Ma hoa API key
- Progress theo tung file va tong tien trinh
- Xuat JSON ra file / share JSON
- Filter media theo album / ngay / loai file

