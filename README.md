# Alpha Skyport - User Management Guide

This guide explains how to manage users in the Alpha Skyport system, including creating the initial admin user and registering regular users.

## 1. Creating the First Admin User

The initial admin user is seeded directly into the database using a Flyway migration script. This ensures that a "Super Admin" exists upon system startup.

### Credentials

- **Email:** `admin@alphaskyport.com`
- **Password:** `admin123`
- **Role:** `SUPER_ADMIN`

### How it works

The migration file `src/main/resources/db/migration/V10__Seed_Admin_User.sql` inserts this user automatically when the application starts.

### Verifying Admin Access

You can verify the admin user by attempting to log in via the Admin API:

```bash
curl -X POST http://localhost:9095/api/admin/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@alphaskyport.com", "password":"admin123"}'
```

**Successful Response:**
You will receive a JSON response containing an `accessToken` and user details.

## 2. Creating Regular Users

Regular users (for the main application, not the admin panel) are created via the public API.

### API Endpoint

- **URL:** `POST http://localhost:9095/api/v1/users`
- **Content-Type:** `application/json`

### Payload Example

```json
{
  "email": "user@example.com",
  "passwordHash": "hashed_password_here",
  "userType": "private",
  "firstName": "John",
  "lastName": "Doe",
  "countryCode": "US"
}
```

### curl Command

```bash
curl -X POST http://localhost:9095/api/v1/users \
     -H "Content-Type: application/json" \
     -d '{
           "email": "john.doe@example.com",
           "passwordHash": "secret123",
           "userType": "private",
           "firstName": "John",
           "lastName": "Doe",
           "countryCode": "US"
         }'
```

> **Note:** The current `UserController` accepts a `passwordHash` directly. Ensure the client application handles password hashing before sending the request.

## 3. Database Tables

- **Admin Users:** `admin_users` table.
- **Regular Users:** `users` table.
