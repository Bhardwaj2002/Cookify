# Cookify

Recipe-sharing web application: user accounts with optional email 2FA, recipes with photos/videos,
filter-based search and recommendations, comments, ratings, subscriptions, chat and moderation.

See [`plan.md`](plan.md) for requirements, architecture and the step-by-step build checklist.

## Features

- **Accounts** — sign up, profile picture, bio, optional email two-factor login, "remember this device", password reset
- **Recipes** — text steps, ingredients, utensils, photos and videos; diet rules enforced by an OOP class hierarchy
  (`Recipe` → `VegRecipe` → `VeganRecipe` / `JainRecipe`, `EggetarianRecipe`, `PescetarianRecipe`, `NonVegRecipe`)
- **Search & filters** — ingredients you have, utensils, budget, time, calories / protein, veg / non-veg (+ detailed diets),
  cuisine, course, difficulty, rating; sort by trending, top rated, newest, quickest, cheapest
- **For You** — recommendations from diet, followed cooks and liked cuisines
- **Social** — comments, 1–5 star ratings, saved recipes, subscriptions with email on new recipes
- **Chat** — real-time one-to-one messaging over WebSocket
- **Moderation** — abusive text blocked; 3 warnings suspend the account and remove its content; admin page
- **Languages** — English, Hindi, Mandarin, Russian, Spanish, French (and works with the browser's Google Translate)
- **Security** — BCrypt, short-lived JWTs + rotating httpOnly refresh cookies, rate limiting, security headers

| Part | Tech |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security (JWT), JPA/Hibernate, Flyway, PostgreSQL |
| Frontend | Plain HTML, CSS and JavaScript (ES modules) — no build step |
| Hosting (planned) | AWS: EC2, RDS PostgreSQL, S3, CloudFront, SES |

## Run locally

Prerequisites: JDK 21, PostgreSQL 14+ running locally.

```bash
# 1. Databases (once)
createdb cookify
createdb cookify_test

# 2. Backend → http://localhost:8080  (Swagger UI: /swagger-ui.html)
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # macOS + Homebrew
./mvnw spring-boot:run

# 3. Frontend → http://localhost:5500
#    Either VS Code "Live Server" (root is preconfigured to /frontend) or:
cd frontend && python3 -m http.server 5500 --bind localhost
```

Open **http://localhost:5500** (use `localhost`, not `127.0.0.1`, so the login cookies are shared with the API).

In development, emails (2FA codes, password-reset links) are printed in the backend console.

### Demo accounts (dev)

On first start the backend loads 5 sample cooks and 39 recipes. All use the password `Cookify2026!`:
`chef_meera`, `chef_arjun`, `chef_lin`, `chef_sofia`, `chef_kabir`.
An admin account `admin` (same password) opens the moderation page at `/pages/admin.html`.
Each sample recipe has a photo from Wikimedia Commons (free licenses) — see [docs/IMAGE_CREDITS.md](docs/IMAGE_CREDITS.md).

## Tests

```bash
cd backend && ./mvnw test      # 225 tests: unit, integration (real Postgres) and WebSocket
```

Rate limiting is on by default and switched off in the `dev` profile so local testing isn't throttled.

Ready-made API requests for each build step live in `backend/http/*.http` (VS Code REST Client).
