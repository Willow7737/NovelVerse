# 🌐 NovelVerse API Documentation

<p align="center">
  <img src="https://img.shields.io/badge/API-v1-blue?style=for-the-badge" alt="API Version">
  <img src="https://img.shields.io/badge/Protocol-HTTPS-brightgreen?style=for-the-badge" alt="Protocol">
  <img src="https://img.shields.io/badge/Format-JSON-orange?style=for-the-badge" alt="Format">
</p>

---

## 📑 Table of Contents

- [Base URL](#-base-url)
- [Authentication](#-authentication)
- [Endpoints](#-endpoints)
  - [Authentication](#authentication)
  - [Novels](#novels)
  - [Chapters](#chapters)
  - [Users & Profiles](#users--profiles)
  - [Reading Progress](#reading-progress)
  - [Comments](#comments)
  - [Bookmarks](#bookmarks)
  - [Ratings](#ratings)
  - [Point Transactions](#point-transactions)
  - [Purchases](#purchases)
  - [Search](#search)
  - [Author Analytics](#author-analytics)
  - [Admin](#admin)
- [Realtime Subscriptions](#-realtime-subscriptions)

---

## 📍 Base URL

```http
https://your-project.supabase.co/rest/v1
```

---

## 🔑 Authentication

All API requests require authentication headers:

| Header | Value | Description |
| :--- | :--- | :--- |
| `apikey` | `your-anon-key` | Your Supabase anon public key |
| `Authorization` | `Bearer <jwt-token>` | User's JWT token (if authenticated) |

---

## 🚀 Endpoints

### Authentication

#### `POST` Sign Up
`POST /auth/v1/signup`

<details>
<summary>📦 Request Body</summary>

```json
{
  "email": "user@example.com",
  "password": "securepassword",
  "data": {
    "username": "johndoe",
    "display_name": "John Doe"
  }
}
```
</details>

<details>
<summary>✅ Response (200 OK)</summary>

```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "created_at": "2024-01-01T00:00:00Z"
  },
  "session": {
    "access_token": "jwt-token",
    "refresh_token": "refresh-token",
    "expires_in": 3600
  }
}
```
</details>

#### `POST` Sign In
`POST /auth/v1/token?grant_type=password`

#### `POST` Sign In with OAuth
`POST /auth/v1/authorize`

#### `POST` Refresh Token
`POST /auth/v1/token?grant_type=refresh_token`

#### `POST` Sign Out
`POST /auth/v1/logout`

---

### Novels

#### `GET` List Novels
`GET /novels?select=*&is_published=eq.true&order=last_updated_at.desc`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `select` | `string` | Columns to return |
| `is_published` | `boolean` | Filter by publish status |
| `status` | `string` | Filter by status (ongoing, completed, etc.) |
| `price_type` | `string` | Filter by price (free, paid, freemium) |
| `order` | `string` | Sort order |
| `limit` | `integer` | Max results |
| `offset` | `integer` | Pagination offset |

<details>
<summary>✅ Response Example</summary>

```json
[
  {
    "id": "uuid",
    "title": "Novel Title",
    "description": "Description...",
    "cover_image_url": "https://...",
    "author_id": "uuid",
    "status": "ongoing",
    "total_chapters": 50,
    "average_rating": 4.5,
    "created_at": "2024-01-01T00:00:00Z"
  }
]
```
</details>

#### `GET` Get Novel Details
`GET /novels?id=eq.{novelId}&select=*,profiles(username,display_name,avatar_url)`

#### `POST` Create Novel (Author)
`POST /novels`

#### `PATCH` Update Novel
`PATCH /novels?id=eq.{novelId}`

#### `DELETE` Delete Novel
`DELETE /novels?id=eq.{novelId}`

---

### Chapters

#### `GET` List Chapters
`GET /chapters?novel_id=eq.{novelId}&is_published=eq.true&order=chapter_number.asc`

#### `GET` Get Chapter Content
`GET /chapters?id=eq.{chapterId}&select=*`

#### `POST` Create Chapter
`POST /chapters`

#### `PATCH` Update Chapter
`PATCH /chapters?id=eq.{chapterId}`

#### `DELETE` Delete Chapter
`DELETE /chapters?id=eq.{chapterId}`

---

### Users & Profiles

#### `GET` Get Current User
`GET /profiles?id=eq.{userId}`

#### `PATCH` Update Profile
`PATCH /profiles?id=eq.{userId}`

#### `GET` Get User Library
`GET /user_library?user_id=eq.{userId}&select=*,novels(*)`

#### `POST` Add to Library
`POST /user_library`

---

### Reading Progress

#### `GET` Get Progress
`GET /reading_progress?user_id=eq.{userId}&novel_id=eq.{novelId}`

#### `POST` Update Progress
`POST /reading_progress`

---

### Comments

#### `GET` List Comments
`GET /comments?novel_id=eq.{novelId}&parent_id=is.null&order=created_at.desc`

#### `POST` Post Comment
`POST /comments`

#### `POST` Reply to Comment
`POST /comments`

#### `POST` Like Comment
`POST /comment_reactions`

---

### Bookmarks

#### `GET` Get Bookmarks
`GET /bookmarks?user_id=eq.{userId}&order=created_at.desc`

#### `POST` Create Bookmark
`POST /bookmarks`

#### `DELETE` Delete Bookmark
`DELETE /bookmarks?id=eq.{bookmarkId}`

---

### Ratings

#### `POST` Rate Novel
`POST /ratings`

#### `PATCH` Update Rating
`PATCH /ratings?user_id=eq.{userId}&novel_id=eq.{novelId}`

---

### Point Transactions

#### `GET` Get Transaction History
`GET /point_transactions?user_id=eq.{userId}&order=created_at.desc`

#### `GET` Get Balance
`GET /profiles?id=eq.{userId}&select=points_balance`

---

### Purchases

#### `POST` Record Purchase
`POST /purchases`

#### `GET` Get Purchases
`GET /purchases?user_id=eq.{userId}&status=eq.completed`

---

### Search

#### `GET` Full-Text Search
`GET /novels?or=(title.ilike.*query*,description.ilike.*query*)&is_published=eq.true`

#### `GET` Search with Filters
`GET /novels?and=(genres.cs.{Fantasy},status.eq.ongoing,average_rating.gte.4)&order=total_views.desc`

---

### Author Analytics

#### `GET` Get Novel Stats
`GET /novel_analytics?novel_id=eq.{novelId}&order=date.desc&limit=30`

#### `GET` Get Author Earnings
`GET /author_earnings?author_id=eq.{userId}`

---

### Admin

#### `GET` Get Reports
`GET /reports?status=eq.pending&order=created_at.desc`

#### `PATCH` Update Report Status
`PATCH /reports?id=eq.{reportId}`

#### `PATCH` Moderate Content
`PATCH /novels?id=eq.{novelId}`

---

## 📡 Realtime Subscriptions

NovelVerse uses Supabase Realtime for instant updates.

### Subscribe to New Comments
```javascript
const channel = supabase
  .channel('comments')
  .on('INSERT', payload => {
    console.log('New comment:', payload.new);
  })
  .subscribe();
```

### Subscribe to Notifications
```javascript
const channel = supabase
  .channel('notifications')
  .on('INSERT', payload => {
    console.log('New notification:', payload.new);
  })
  .subscribe();
```

---

<p align="center">
  Need help? Join our <a href="#">Discord Community</a> or email <a href="mailto:support@novelverse.app">support@novelverse.app</a>
</p>