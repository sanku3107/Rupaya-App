# Rupaya FastAPI Backend — Full Specification for Java Spring Boot Migration

This document describes the FastAPI backend in full detail so it can be reimplemented as a Java Spring Boot application. It covers entities, API routes, services, security, and configuration.

---

## Table of Contents

1. [Overview & Tech Stack](#1-overview--tech-stack)
2. [Configuration & Environment](#2-configuration--environment)
3. [Database Schema (Entities)](#3-database-schema-entities)
4. [Request/Response Models (DTOs)](#4-requestresponse-models-dtos)
5. [API Routes (Endpoints)](#5-api-routes-endpoints)
6. [Services & Business Logic](#6-services--business-logic)
7. [Authentication & Security](#7-authentication--security)
8. [Exception Handling](#8-exception-handling)
9. [Spring Boot Mapping Notes](#9-spring-boot-mapping-notes)

---

## 1. Overview & Tech Stack

- **Framework:** FastAPI
- **ORM:** SQLAlchemy 2.x (async with `asyncpg`)
- **Database:** PostgreSQL (UUID primary keys, `gen_random_uuid()`, `now()` for timestamps)
- **Cache/Token blacklist:** Redis (async client)
- **Auth:** JWT (access + refresh), OAuth2 password bearer, bcrypt for passwords
- **Validation:** Pydantic v2
- **API base path:** `/api/v1` (configurable via `api_base_path`)

**Optional / unused in current code:** Supabase client is instantiated in `app/db/supabase_client.py` but not used by any router or service. Config does not define `SUPABASE_URL`/`SUPABASE_KEY`; you can omit Supabase in Spring Boot unless you plan to use it.

---

## 2. Configuration & Environment

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `SECRET_KEY` | string | `"super_secret_key"` | JWT signing secret |
| `DATABASE_URL` | string | **required** | PostgreSQL URL, e.g. `postgresql://user:pass@host:5432/db` |
| `REDIS_URL` | string | **required** | Redis URL, e.g. `redis://localhost:6379` |
| `PORT` | int | `8000` | Server port |
| `HOST` | string | `"0.0.0.0"` | Bind host |
| `api_base_path` | string | `"/api/v1"` | All API routes are under this prefix |
| `access_token_expire_minutes` | int | `60` | Not used directly; see below |
| `JWT_ALGORITHM` | string | `"HS256"` | JWT algorithm |
| `ACCESS_TOKEN_EXPIRE` | timedelta | 1 hour | Access token TTL |
| `REFRESH_TOKEN_EXPIRE` | timedelta | 7 days | Refresh token TTL |

**CORS:** `ALLOWED_ORIGINS` env can be set (comma-separated). If not set or `*`, the app allows `http://localhost:3000`, `http://localhost:3001`, `http://127.0.0.1:3000`, and `*`.

---

## 3. Database Schema (Entities)

All tables use **soft delete** via `deleted_at` and `deleted_by` (nullable). Queries filter with `deleted_at IS NULL` unless otherwise noted.

### 3.1 Enums

| Enum | Values |
|------|--------|
| `Role` | `USER`, `ADMIN`, `SUPER_ADMIN` |
| `GroupRole` | `ADMIN`, `MEMBER` |
| `SplitType` | `EQUAL`, `EXACT` |

**Note:** Pydantic model `SplitType` in `app/models/bills.py` also has `PERCENTAGE`, but the DB and services only use `EQUAL` and `EXACT`. For Spring Boot, implement only `EQUAL` and `EXACT` unless you add PERCENTAGE logic.

### 3.2 Table: `User`

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| `id` | UUID | NO | `gen_random_uuid()` | PK |
| `name` | String | NO | - | |
| `email` | String | NO | - | Unique, indexed |
| `password` | String | NO | - | Bcrypt hash |
| `role` | Role (enum) | YES | `USER` | |
| `created_by` | UUID | YES | - | FK → User.id, ON DELETE SET NULL |
| `updated_by` | UUID | YES | - | FK → User.id, ON DELETE SET NULL |
| `deleted_by` | UUID | YES | - | FK → User.id, ON DELETE SET NULL |
| `created_at` | Timestamp (tz) | NO | `now()` | |
| `updated_at` | Timestamp (tz) | YES | - | |
| `deleted_at` | Timestamp (tz) | YES | - | Soft delete |

**Indexes:** `email` (unique), `created_by`, `updated_by`, `deleted_by`.

### 3.3 Table: `Group`

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| `id` | UUID | NO | `gen_random_uuid()` | PK |
| `name` | String | NO | - | Indexed |
| `description` | String | YES | - | |
| `created_by` | UUID | YES | - | FK → User.id, SET NULL |
| `updated_by` | UUID | YES | - | FK → User.id, SET NULL |
| `deleted_by` | UUID | YES | - | FK → User.id, SET NULL |
| `created_at` | Timestamp (tz) | NO | `now()` | |
| `updated_at` | Timestamp (tz) | YES | - | |
| `deleted_at` | Timestamp (tz) | YES | - | Soft delete |

**Indexes:** `name`, `created_by`, `updated_by`, `deleted_by`.

### 3.4 Table: `GroupMember`

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| `id` | UUID | NO | `gen_random_uuid()` | PK |
| `user_id` | UUID | NO | - | FK → User.id, ON DELETE CASCADE |
| `group_id` | UUID | NO | - | FK → Group.id, ON DELETE CASCADE |
| `created_by` | UUID | NO | - | FK → User.id, ON DELETE RESTRICT |
| `updated_by` | UUID | YES | - | FK → User.id, SET NULL |
| `deleted_by` | UUID | YES | - | FK → User.id, SET NULL |
| `role` | GroupRole (enum) | YES | `MEMBER` | |
| `created_at` | Timestamp (tz) | NO | `now()` | |
| `updated_at` | Timestamp (tz) | YES | - | |
| `deleted_at` | Timestamp (tz) | YES | - | Soft delete |

**Unique constraint:** `(user_id, group_id)` — one membership per user per group.

**Indexes:** `user_id`, `group_id`, `created_by`, `updated_by`, `deleted_by`.

### 3.5 Table: `Bill`

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| `id` | UUID | NO | `gen_random_uuid()` | PK |
| `group_id` | UUID | NO | - | FK → Group.id, ON DELETE CASCADE |
| `paid_by` | UUID | NO | - | FK → User.id, ON DELETE RESTRICT |
| `created_by` | UUID | NO | - | FK → User.id, ON DELETE RESTRICT |
| `updated_by` | UUID | YES | - | FK → User.id, SET NULL |
| `deleted_by` | UUID | YES | - | FK → User.id, SET NULL |
| `description` | String | NO | - | |
| `total_amount` | Float | NO | - | |
| `split_type` | SplitType (enum) | YES | `EQUAL` | |
| `created_at` | Timestamp (tz) | NO | `now()` | |
| `updated_at` | Timestamp (tz) | YES | - | |
| `deleted_at` | Timestamp (tz) | YES | - | Soft delete |

**Indexes:** `group_id`, `paid_by`, `created_by`, `updated_by`, `deleted_by`.

### 3.6 Table: `BillShare`

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| `id` | UUID | NO | `gen_random_uuid()` | PK |
| `bill_id` | UUID | NO | - | FK → Bill.id, ON DELETE CASCADE |
| `user_id` | UUID | NO | - | FK → User.id, ON DELETE CASCADE |
| `created_by` | UUID | YES | - | FK → User.id, SET NULL |
| `updated_by` | UUID | YES | - | FK → User.id, SET NULL |
| `deleted_by` | UUID | YES | - | FK → User.id, SET NULL |
| `amount` | Float | NO | - | Share amount for this user |
| `paid` | Boolean | YES | `false` | Whether this share has been settled |
| `created_at` | Timestamp (tz) | NO | `now()` | |
| `updated_at` | Timestamp (tz) | YES | - | |
| `deleted_at` | Timestamp (tz) | YES | - | Soft delete |

**Unique constraint:** `(bill_id, user_id)` — one share per user per bill.

**Indexes:** `bill_id`, `user_id`, `created_by`, `updated_by`, `deleted_by`.

---

## 4. Request/Response Models (DTOs)

These are Pydantic models. In Spring Boot they map to DTOs/records and validation annotations.

### 4.1 Auth (`app/models/auth.py`)

| Model | Usage | Fields |
|-------|--------|--------|
| `TokenResponse` | Response | `access_token: str`, `refresh_token: str`, `token_type: str` (default `"bearer"`) |
| `RefreshTokenRequest` | Body | `refresh_token: str` |
| `PasswordChangeRequest` | Body | `old_password: str`, `new_password: str` |

**Login:** Uses standard OAuth2 form: `username` (email), `password`.

### 4.2 Users (`app/models/users.py`)

| Model | Usage | Fields |
|-------|--------|--------|
| `UserCreate` | Register body | `name: str`, `email: EmailStr`, `password: str` |
| `UserLogin` | Not used in routes (login uses form) | `email`, `password` |
| `UserOut` | Response | `id: UUID`, `name: str`, `email: str`, `role: Role` |

### 4.3 Groups (`app/models/groups.py`)

| Model | Usage | Fields |
|-------|--------|--------|
| `GroupCreate` | Create group body | `name: str`, `description: str \| null`, `initial_members: list[str]` (emails), default `[]` |
| `GroupUpdate` | Update group body | `name: str \| null`, `description: str \| null` |
| `GroupOut` | List/detail response | `id`, `name`, `description`, `created_by`, `created_at`, `member_count`, `total_owed`, `total_owe` |
| `GroupMemberOut` | Member in detail | `id`, `user: UserOut`, `role: str`, `created_at` |
| `GroupDetailOut` | Group detail response | Extends `GroupOut`; adds `members: list[GroupMemberOut]` |
| `AddMemberRequest` | Add member body | `email: str`, `role: str` (default `"MEMBER"`) |
| `MemberUpdate` | Update member role body | `role: str` |

### 4.4 Bills (`app/models/bills.py`)

| Model | Usage | Fields |
|-------|--------|--------|
| `BillShareBase` | Base for share | `user_id: UUID`, `amount: float \| null` (≥0, for EXACT) |
| `BillShareCreate` | Share in create/update | Same as base |
| `BillShareResponse` | Share in response | `id`, `user_id`, `amount`, `paid`, `user: UserOut` |
| `BillBase` | Base | `description: str`, `total_amount: float` (gt 0) |
| `BillCreate` | Create bill body | `description`, `total_amount`, `group_id: UUID`, `paid_by: UUID \| null`, `split_type: SplitType` (default EQUAL), `shares: list[BillShareCreate]` |
| `BillUpdate` | Update bill body | All optional: `description`, `total_amount` (gt 0), `paid_by`, `split_type`, `shares` |
| `GroupMinimal` | Nested in bill response | `id: UUID`, `name: str` |
| `BillResponse` | Bill response | `id`, `group_id`, `paid_by`, `payer: UserOut \| null`, `group: GroupMinimal \| null`, `created_by`, `created_at`, `split_type`, `description`, `total_amount`, `shares: list[BillShareResponse]` |

### 4.5 Pagination (`app/models/pagination.py`)

| Model | Usage | Fields |
|-------|--------|--------|
| `PaginatedResponse<T>` | Generic list response | `items: List[T]`, `total: int`, `skip: int`, `limit: int`, `has_more: bool` |

---

## 5. API Routes (Endpoints)

Base path for all below: **`/api/v1`**. Auth uses Bearer token unless stated otherwise.

**Global:**  
- `GET /` — not under API base: `{"message": "Rupaya API running 🚀", "docs": "/docs", "version": "v1"}`  
- `GET /health` and `GET /api/v1/health` — `{"status": "healthy", "service": "rupaya-api"}`  

---

### 5.1 Auth — Prefix `/api/v1/auth`

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| POST | `/login` | No | Form: `username` (email), `password` | `TokenResponse` | Login; returns access + refresh tokens |
| POST | `/logout` | Bearer | - | `{ "detail": "Logged out successfully" }` | Blacklist current access token in Redis |
| POST | `/logout-all` | Bearer | - | `{ "detail": "All sessions revoked" }` | Blacklist current token + set revoke timestamp for user (invalidates all tokens issued before now) |
| POST | `/refresh` | No | Body: `RefreshTokenRequest` | `{ "access_token", "token_type": "bearer" }` | Issue new access token from refresh token |

---

### 5.2 Users — Prefix `/api/v1/users`

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| POST | `/register` | No | Body: `UserCreate` | `UserOut` | Register; password min 8 chars; email unique |
| GET | `/me` | Bearer | - | `UserOut` | Current user profile |
| POST | `/change-password` | Bearer | Body: `PasswordChangeRequest` | `{ "detail": "Password changed successfully" }` | Change password; new password min 8 chars |
| GET | `/search` | Bearer | Query: `q` (string) | `list[UserOut]` | Search users by name or email (case-insensitive), exclude current user; limit 10 |

---

### 5.3 Groups — Prefix `/api/v1/groups`

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| POST | `/` | Bearer | Body: `GroupCreate` | `GroupOut` | Create group; must have at least one other member (by email); creator is ADMIN |
| GET | `/` | Bearer | Query: `search`, `filter`, `sort_by`, `order`, `skip`, `limit` | `PaginatedResponse[GroupOut]` | List groups for current user; filter: `owe` \| `owed`; sort_by: `name` \| `owed` \| `owe` \| `created_at`; order: `asc` \| `desc`; default sort `created_at` desc |
| GET | `/{group_id}` | Bearer | - | `GroupDetailOut` | Group detail with members; user must be member |
| POST | `/{group_id}/members` | Bearer | Body: `AddMemberRequest` | `GroupMemberOut` | Add member by email; caller must be group ADMIN |
| DELETE | `/{group_id}/members/{member_id}` | Bearer | - | (no body) | Remove member (soft delete); caller must be ADMIN or removing self |
| PATCH | `/{group_id}` | Bearer | Body: `GroupUpdate` | `GroupOut` | Update group; caller must be member |
| PATCH | `/{group_id}/members/{member_id}` | Bearer | Body: `MemberUpdate` | `GroupMemberOut` | Update member role; caller must be ADMIN |
| DELETE | `/{group_id}` | Bearer | - | (no body) | Soft delete group and all its members and bills; caller must be ADMIN |

---

### 5.4 Bills — Prefix `/api/v1/bills`

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| POST | `/` | Bearer | Body: `BillCreate` | `BillResponse` (201) | Create bill in group; user must be member; `paid_by` defaults to creator |
| GET | `/` | Bearer | Query: `skip`, `limit` | `PaginatedResponse[BillResponse]` | All bills where user is payer or in shares |
| GET | `/group/{group_id}` | Bearer | Query: `skip`, `limit`, `search` | `PaginatedResponse[BillResponse]` | Bills in group; user must be member; optional search on description |
| GET | `/{bill_id}` | Bearer | - | `BillResponse` | Bill detail; user must be in group |
| PATCH | `/{bill_id}` | Bearer | Body: `BillUpdate` | `BillResponse` | Update bill; user must be member; can recalc shares for EQUAL |
| PATCH | `/shares/{share_id}/mark-paid` | Bearer | - | `BillShareResponse` | Mark share as paid; only the user who owes the share |
| PATCH | `/shares/{share_id}/mark-unpaid` | Bearer | - | `BillShareResponse` | Mark share as unpaid; only the user who owes the share |

---

### 5.5 Summary — Prefix `/api/v1/summary`

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| GET | `/` | Bearer | Query: `group_id` (optional UUID) | See below | User summary; if `group_id` given, scope to that group and validate membership |

**Summary response body:**

```json
{
  "total_owed": 0.0,
  "total_owe": 0.0,
  "group_count": 0,
  "friends": [ { "id", "name", "email" } ]
}
```

- `total_owed`: Sum of unpaid shares where current user is payer (others owe you).  
- `total_owe`: Sum of unpaid shares where current user is debtor (you owe others).  
- `group_count`: Number of groups (1 if `group_id` provided, else total).  
- `friends`: Only when `group_id` not provided; up to 5 distinct users from same groups (exclude self).

---

## 6. Services & Business Logic

### 6.1 AuthService (`app/services/auth_service.py`)

- **Constructor:** `AuthService(db: AsyncSession)`.
- **Used by:** Auth router; also provides `get_current_user` dependency (uses DB + Redis).

**Methods:**

| Method | Inputs | Returns | Logic |
|--------|--------|---------|--------|
| `login_user` | `email`, `password` | Dict with `access_token`, `refresh_token`, `token_type` | Find user by email; verify password (bcrypt); create access (1h) and refresh (7d) JWTs with `sub=user.id`, `type=access`/`refresh`; return tokens. On failure: `UnauthorizedError("Invalid email or password")`. |
| `logout_user` | `token` (Bearer) | `{ "detail": "Logged out successfully" }` | Decode JWT; set Redis key `blacklist:{token}` with TTL = remaining token expiry. On decode error: `ValidationError("Invalid token")`. |
| `logout_all_sessions` | `token` | `{ "detail": "All sessions revoked" }` | Resolve user from token; blacklist current token; set Redis `revoke_all:{user.id}` to current timestamp (invalidates all tokens issued before this). |
| `refresh_access_token` | `refresh_token` (body) | `{ "access_token", "token_type": "bearer" }` | If `blacklist:{refresh_token}` exists → Unauthorized. Decode; require `type==refresh` and `sub`; if `revoke_all:{user_id}` > token `iat` → Unauthorized; load user; if not found → Unauthorized; issue new access token only. |
| `get_current_user` | `token` (from header), `db` | User entity | If `blacklist:{token}` exists → Unauthorized. Decode; check `revoke_all:{user_id}` vs `iat`; load user by id; if not found → Unauthorized. Return user. |

**JWT payload:** `sub` (user id string), `type` (`access` or `refresh`), `iat`, `exp`.

---

### 6.2 UserService (`app/services/user_service.py`)

- **Constructor:** `UserService(db)`.

| Method | Inputs | Returns | Logic |
|--------|--------|---------|--------|
| `register_user` | `name`, `email`, `password` | User entity | Password length ≥ 8 else ValidationError; email must not exist else ConflictError; hash password (bcrypt); create User; commit; return user. |
| `change_user_password` | `user_id`, `old_password`, `new_password` | `{ "detail": "Password changed successfully" }` | New password ≥ 8 else ValidationError; load user else NotFoundError; verify old password else ValidationError; hash new password; update and commit. |
| `get_user_by_id` | `user_id` | User | Load user; NotFoundError if missing. |
| `search_users` | `query`, `current_user_id` | List of User | Filter: `id != current_user_id`, name or email ILIKE `%query%`; limit 10. |

---

### 6.3 GroupService (`app/services/group_service.py`)

- **Constructor:** `GroupService(db)`.
- **Helpers:** `check_is_member(user_id, group_id)` → membership or ForbiddenError; `check_is_admin(user_id, group_id)` → same but role must be ADMIN.

| Method | Inputs | Returns | Logic |
|--------|--------|---------|--------|
| `create_group` | `data: GroupCreate`, `creator_id` | Group | If `initial_members` empty → ValidationError. Create Group; flush; add creator as GroupMember (ADMIN); for each email in `initial_members`, find user, skip if self, add as MEMBER; if no one added → rollback + ValidationError; commit; return group. |
| `get_user_groups` | `user_id`, `search`, `filter`, `sort_by`, `order`, `skip`, `limit` | `{ items, total, skip, limit, has_more }` | Load memberships (with group, members) where user_id and not deleted; for each group compute `total_owed` (sum BillShare.amount where Bill.paid_by=user, BillShare.user_id≠user, unpaid) and `total_owe` (sum where paid_by≠user, BillShare.user_id=user, unpaid); filter: if `filter==owe` drop groups where total_owe≤total_owed; if `filter==owed` drop where total_owed≤total_owe; sort by sort_by (name, owed, owe, created_at) and order; paginate with skip/limit. |
| `get_group_detail` | `group_id`, `user_id` | `GroupDetailOut` | check_is_member; load group with members and users; filter members by deleted_at; build GroupMemberOut list; compute total_owed/total_owe for user in group; return GroupDetailOut. |
| `add_member_to_group` | `group_id`, `data: AddMemberRequest`, `added_by_id` | GroupMember | check_is_admin; find user by email (NotFoundError); if already active member → ValidationError; if soft-deleted membership, reactivate and set role; else create new GroupMember; commit; return member with user loaded. |
| `remove_member_from_group` | `group_id`, `member_id`, `removed_by_id` | - | Load member; NotFoundError if missing; if member.user_id != removed_by_id then check_is_admin; soft delete membership (deleted_at, deleted_by); commit. |
| `update_group` | `group_id`, `data: GroupUpdate`, `user_id` | Group | check_is_member; load group; apply name/description if present; set updated_at, updated_by; commit. |
| `update_member_role` | `group_id`, `member_id`, `role`, `user_id` | GroupMember | check_is_admin; load member for group; set role, updated_at, updated_by; commit. |
| `delete_group` | `group_id`, `user_id` | `{ "message": "Group deleted successfully" }` | check_is_admin; soft delete group (deleted_at, deleted_by); soft delete all GroupMember and Bill for group; commit. |

---

### 6.4 BillService (`app/services/bill_service.py`)

- **Constructor:** `BillService(group_service: GroupService)`; `db` is taken from `group_service.db`.
- **Core helper:** `_calculate_shares(split_type, total_amount, shares_input, paid_by)` → list of `{ user_id, amount, paid }`.

**_calculate_shares logic:**

- **EQUAL:** Consider “involved” shares as those with no amount or amount > 0. Count = number of involved; per-person amount = total_amount / count. For each share: amount = that value if involved else 0; paid = (share.user_id == paid_by). At least one involved else ValidationError.
- **EXACT:** Sum of share amounts must equal total_amount (within 0.01) else ValidationError. For each share: amount = share.amount or 0; paid = (share.user_id == paid_by).

| Method | Inputs | Returns | Logic |
|--------|--------|---------|--------|
| `create_bill` | `user_id`, `data: BillCreate` | BillResponse | check_is_member(group_id); paid_by = data.paid_by or user_id; shares = _calculate_shares(...); create Bill and BillShare rows; commit; return get_bill_details. |
| `update_bill` | `user_id`, `bill_id`, `data: BillUpdate` | BillResponse | Load bill with shares; check_is_member(group_id); if data.shares provided, recalc with _calculate_shares; else if only total/split_type/paid_by changed and split_type is EQUAL, recalc from existing share user IDs; for EXACT, if total_amount changed without new shares and sum(shares) ≠ new total → ValidationError; update bill fields; sync shares: remove users not in new list, update or insert others; commit; return get_bill_details. |
| `get_group_bills` | `user_id`, `group_id`, `skip`, `limit`, `search` | Paginated | check_is_member; query Bill by group_id, deleted_at null; optional filter description ILIKE search; count; fetch with shares, payer, group; order created_at desc; offset/limit. |
| `get_user_bills` | `user_id`, `skip`, `limit` | Paginated | Bills where (paid_by=user or user in shares) and not deleted; count; fetch with shares, payer, group; order created_at desc; offset/limit. |
| `get_bill_details` | `user_id`, `bill_id` | Bill | Load bill with shares, users, payer, group; NotFoundError if missing; check_is_member(bill.group_id); return bill. |
| `mark_share_as_paid` | `user_id`, `share_id` | BillShare | Load share with bill and user; NotFoundError; check_is_member(bill.group_id); if share.user_id != user_id → ForbiddenError; if already paid → ValidationError; set paid=true, updated_at, updated_by; commit; return share. |
| `mark_share_as_unpaid` | `user_id`, `share_id` | BillShare | Same as paid but: only allow if share.user_id == user_id; if not paid → ValidationError; set paid=false; commit. |

---

### 6.5 SummaryService (`app/services/summary_service.py`)

- **Constructor:** `SummaryService(group_service)`; `db` from group_service.

| Method | Inputs | Returns | Logic |
|--------|--------|---------|--------|
| `get_user_summary` | `user_id`, `group_id` (optional) | `{ total_owed, total_owe, group_count, friends }` | If group_id: check_is_member. group_count: 1 if group_id else count of GroupMember for user (not deleted). total_owed: sum(BillShare.amount) where Bill.paid_by=user, BillShare.user_id≠user, unpaid; optional filter by group_id. total_owe: sum where paid_by≠user, BillShare.user_id=user, unpaid; same group filter. friends: only if no group_id; users in same groups as current user, exclude self, distinct, limit 10; return first 5 as list of { id, name, email }. |

---

## 7. Authentication & Security

- **Password hashing:** bcrypt (passlib with bcrypt).
- **Tokens:** JWT, HS256, secret from `SECRET_KEY`. Access: 1 hour; refresh: 7 days. Payload: `sub` (user id), `type` (`access`/`refresh`), `iat`, `exp`.
- **OAuth2:** Password bearer; token in `Authorization: Bearer <token>`. Login form: `username` (email) + `password`.
- **Redis:**
  - `blacklist:{token}` — any token (access or refresh) that is logged out; TTL = remaining token expiry. Checked on every authenticated request and on refresh.
  - `revoke_all:{user_id}` — timestamp; any token with `iat` < this value is considered invalid. Set on “logout all”.
- **Current user:** Resolved in `get_current_user`: decode JWT, check blacklist and revoke_all, load user by `sub`; 401 if invalid or missing.

**Endpoints that do not require auth:**  
- `POST /api/v1/auth/login`  
- `POST /api/v1/auth/refresh`  
- `POST /api/v1/users/register`  
- `GET /`, `GET /health`, `GET /api/v1/health`  

All other API routes use `get_current_user` (Bearer token required).

---

## 8. Exception Handling

Custom exception hierarchy; all extend `RupayaException(message)`.

| Exception | HTTP Status | When |
|-----------|-------------|------|
| `NotFoundError` | 404 | Resource not found (user, group, member, bill, share). |
| `UnauthorizedError` | 401 | Invalid login, invalid/expired/blacklisted token, wrong token type, user gone. |
| `ForbiddenError` | 403 | Not member, not admin when required. |
| `ConflictError` | 409 | Email already registered. |
| `ValidationError` | 400 | Business validation (password length, share sum, etc.). |

Response body: `{ "detail": "<message>" }`.

---

## 9. Spring Boot Mapping Notes

- **Entities:** Map tables to JPA entities; use `@Enumerated(EnumType.STRING)` for Role, GroupRole, SplitType. Use soft delete (`deleted_at != null`) in `@Where` or queries.
- **IDs:** Use `UUID` type and `@GeneratedValue` with UUID generator or database default.
- **DTOs:** Use records or POJOs with Bean Validation; separate request/response for each endpoint as in the table above.
- **Pagination:** `skip`/`limit` → Spring’s `Pageable` (e.g. `page = skip/limit`, `size = limit`) or pass offset/limit to repository.
- **Auth:** Spring Security with JWT filter; validate token then check Redis blacklist and revoke_all; load user and set in SecurityContext. Use bcrypt for passwords.
- **Redis:** Use RedisTemplate or reactive client; key patterns `blacklist:*`, `revoke_all:*`; set TTL for blacklist keys.
- **CORS:** Mirror allowed origins and credentials from FastAPI config.
- **API prefix:** Map all REST controllers under `/api/v1`.
- **SplitType:** Implement only EQUAL and EXACT to match current DB and services; add PERCENTAGE later if needed.
- **Group create:** `initial_members` is list of email strings; at least one must resolve to another user.
- **Bills:** Share calculation logic must match `_calculate_shares` (EQUAL vs EXACT, payer gets paid=true, validation of sums).

This spec should be enough to reimplement the Rupaya backend in Java Spring Boot with equivalent behavior, entities, API contracts, and business rules.
