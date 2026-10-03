# Cookify — Project Plan

A recipe-sharing web application with user accounts, filter-based recommendations, comments/ratings, subscriptions, built-in chat, moderation (ban system) and multi-language support.

Source requirements: assignment brief + `Interview technical assigment.pdf` (Gantt, DB tables, ERD, UML, flowcharts, pseudocode, UI mockups, test table).

---

## 1. Requirements Summary

### Functional
| # | Requirement | Notes from PDF |
|---|---|---|
| 1 | Sign up, username, profile picture, bio | Username must be unique; password ≥ 9 chars, no spaces; confirm password twice |
| 2 | Login with optional 2FA | 6-digit OTP emailed to user |
| 3 | Remember login on device | "Session remembered" — trusted-device cookie |
| 4 | Forgot / reset password | Reset link emailed |
| 5 | Recipes with text steps + photo/video | Drag-and-drop upload, numbered steps |
| 6 | Search & filter | Ingredients, utensils/equipment, cost, time, calories/nutrition, veg/non-veg (+ eggetarian, pescetarian, Jain, vegan), popularity, cuisine/region, course, difficulty |
| 7 | Recommendations | Based on diet preference + followed creators + popularity |
| 8 | Comments & ratings | 1–5 stars, averaged; NSFW comments blocked; owner emailed |
| 9 | Ban system | Blocked comment → warning (stored + emailed); 3 warnings → ban; banned user's content removed |
| 10 | Subscriptions | Follow a creator; followers emailed on new recipe; owner emailed on new subscriber |
| 11 | Built-in chat | User-to-user direct messages |
| 12 | Multi-language | English + Hindi, Mandarin, Russian, Spanish, French; Google Translate compatible |
| 13 | Profile page | Avatar, name, username, followers/following, uploaded & saved recipe carousels |

### Non-functional
- Clean, modular, scalable codebase with OOP (inheritance, classes, polymorphism) for recipe categorisation.
- Modern, appealing UI (fonts, colours, sliders, toggles, interactive elements), responsive.
- Secure (hashed passwords, httpOnly cookies, rate limiting, input validation).
- Zero/low cost: free tiers only.
- Hosted on a cloud platform; source on GitHub; screenshots provided.

---

## 2. Development Approach

### Methodology
Iterative, feature-sliced delivery. Each phase produces a working, demoable vertical slice (DB → API → UI) and is checked against the PDF test table before moving on.

### Principles
- **Layered architecture:** `routes → controllers → services → repositories → database`. Controllers stay thin; business logic lives in services; SQL lives only in repositories.
- **OOP where it adds value:** recipe diet hierarchy, filter classes, sort strategies, notifiers (see §4.4).
- **Config via environment variables** (`.env`, never committed) so the same code runs locally and in the cloud.
- **Validation at the edge:** every request body/query validated before reaching services.
- **Consistent error handling:** central error middleware returning `{ error: { code, message } }`.
- **Conventional commits** and small PR-sized commits on `main` (feature branches if collaborating).

### Phases & Timeline
| Phase | Scope | Est. |
|---|---|---|
| 1. Setup | Repo, Spring Boot skeleton (JDK 21 + Maven wrapper), Flyway migrations, seed data (~30 recipes), design tokens | 0.5 day |
| 2. Auth & Profile | Sign up, login, bcrypt, JWT + refresh cookie, email OTP 2FA, trusted devices, forgot/reset password, avatar + bio | 1.5 days |
| 3. Recipes | Upload/edit with image/video, steps editor, OOP recipe hierarchy, recipe view page, view counter | 1.5 days |
| 4. Search & Recommendations | Filter classes, sliders/toggles UI, popularity sort, "For You" feed | 1 day |
| 5. Social | Comments (profanity filter), ratings, save recipe, follow/subscribe, email notifications | 1 day |
| 6. Moderation | Warnings, auto-ban at 3, admin ban/unban, content removal | 0.5 day |
| 7. Chat | WebSocket/STOMP DMs, conversation list, unread badges, message history | 1 day |
| 8. i18n | Locale JSON for 6 languages, language switcher, `lang` attributes for Google Translate | 0.5 day |
| 9. Polish & Security | Responsive pass, animations, empty/loading states, helmet, rate limits, CSRF-safe cookies | 1 day |
| 10. Deploy & Docs | AWS (EC2, RDS, S3, CloudFront, SES), README, screenshots per test case | 0.5 day |

**Total: ~9 working days.**

### Testing Strategy
- **Unit tests** (JUnit 5 + Mockito): services, filter classes, recipe subclasses, password/comment validators.
- **Integration tests** (Spring Boot Test + MockMvc + Testcontainers Postgres): auth flow, recipe CRUD, search filters, ban logic.
- **Manual acceptance:** walk through all 13 cases in the PDF test table; capture a screenshot for each into `docs/screenshots/`.

---

## 3. Frontend Approach

### Stack
- **Plain HTML5, CSS3, JavaScript (ES modules)** — no framework, as required.
- Multi-page app: one HTML file per page, one JS module per page, shared components.
- Native `fetch` wrapper (`api.js`) handling JSON, auth refresh and errors.
- **STOMP.js + SockJS client** (from CDN) for chat.
- No build step; deployed as static files to S3 and served via CloudFront. During development, served by VS Code Live Server and calls the local API.

### Design System (from PDF mockups)
| Token | Value | Use |
|---|---|---|
| `--cream` | `#FAF8EF` | Page background |
| `--amber` | `#F4BE6A` | Primary buttons, cards |
| `--rust` | `#C26D37` | Footer, accents, inverted cards |
| `--brown-dark` | `#5A3A22` | Text |
| `--grey-input` | `#A6A6A6` | Input pills |
| Display font | **Fraunces** (Google Fonts) | Logo, headings |
| Body font | **Poppins** | UI text |

- Pill-shaped inputs and buttons, rounded cards, soft shadows, micro-animations on hover/click.
- Dark mode via CSS variables (optional stretch).
- Mobile-first responsive layout (CSS Grid + Flexbox).

### Pages
| Page | Key elements |
|---|---|
| Landing | Logo, Login / Sign Up CTAs, trending recipes, footer (Contact Us, About) |
| Sign Up | Username, email, phone, password ×2, live validation, "already in use" message |
| Login | Identifier (username/email/phone), password, 2FA OTP step, "Remember this device" |
| Forgot / Reset Password | Email request → reset link → new password |
| Browse / Search | Search bar, ingredient chips, equipment chips, **sliders** (cost, time, calories), **star selectors** (speed, difficulty), veg/non-veg **toggle** + diet dropdown, cuisine/course dropdowns, sort selector, recipe card grid, pagination |
| Recipe View | Media gallery (image/video), title, author + Subscribe, date, views, ingredients, numbered steps, nutrition, star rating, comments |
| Recipe Upload / Edit | Drag-and-drop media, title, dynamic step list, ingredients, equipment, diet, cost, time, calories, difficulty |
| Profile | Avatar, name, username, bio, followers/following, Subscribe, carousels (uploaded, saved) |
| Edit Profile / Settings | Avatar upload, bio, diet preferences, 2FA toggle, trusted devices, language |
| Chat | Conversation list, message thread, real-time updates |
| Admin | Reported users, warnings, ban/unban |

### Structure
See §4 Project Structure → `frontend/`.

### Multi-language
- All UI strings use `data-i18n="key"` attributes; `i18n.js` swaps text from `locales/<lang>.json`.
- Language selector in header; choice saved in `localStorage` and user profile.
- `<html lang="…">` updated on switch so Google Translate also works on user-generated content.

---

## 4. Project Structure

Monorepo with three deployable parts: `backend/` (Spring Boot API), `frontend/` (static site), `infra/` (AWS config and scripts).

```
aditya/
├── plan.md
├── README.md
├── .gitignore
├── .github/workflows/            # ci.yml (build + test), deploy.yml (backend → EC2, frontend → S3)
│
├── backend/                      # Java 21 + Spring Boot 4 (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/cookify/
│       │   │   ├── CookifyApplication.java
│       │   │   ├── config/               # SecurityConfig, WebSocketConfig, AwsConfig, CorsConfig, AsyncConfig
│       │   │   ├── common/
│       │   │   │   ├── exception/        # ApiException, NotFoundException, GlobalExceptionHandler
│       │   │   │   ├── response/         # ApiResponse, PageResponse
│       │   │   │   └── util/             # PasswordRules, SlugUtil
│       │   │   ├── security/
│       │   │   │   ├── jwt/              # JwtService, JwtAuthFilter
│       │   │   │   ├── otp/              # OtpService (email 2FA codes)
│       │   │   │   └── device/           # TrustedDeviceService ("remember this device")
│       │   │   ├── auth/                 # signup, login, verify-otp, refresh, logout, forgot/reset password
│       │   │   │   ├── controller/  service/  dto/
│       │   │   ├── user/                 # profile, avatar, bio, preferences
│       │   │   │   ├── controller/  service/  repository/  entity/  dto/
│       │   │   ├── recipe/
│       │   │   │   ├── controller/  service/  repository/  entity/  dto/
│       │   │   │   ├── domain/           # Recipe (abstract) + VegRecipe, VeganRecipe, JainRecipe,
│       │   │   │   │                     #   EggetarianRecipe, PescetarianRecipe, NonVegRecipe, RecipeFactory
│       │   │   │   ├── filter/           # RecipeFilter + IngredientFilter, EquipmentFilter, BudgetFilter,
│       │   │   │   │                     #   TimeFilter, NutritionFilter, DietFilter, CuisineFilter, ...
│       │   │   │   └── sort/             # SortStrategy + PopularitySort, TopRatedSort, NewestSort, QuickestSort
│       │   │   ├── social/
│       │   │   │   ├── comment/  rating/  follow/  saved/
│       │   │   ├── recommendation/       # RecommendationService ("For You" feed)
│       │   │   ├── moderation/           # ProfanityFilter, WarningService, BanService
│       │   │   ├── chat/                 # STOMP controllers, Conversation, Message
│       │   │   ├── notification/         # Notifier (abstract) → SesEmailNotifier, ConsoleNotifier
│       │   │   ├── media/                # StorageService → S3StorageService, LocalStorageService
│       │   │   └── admin/                # ban/unban, warnings review
│       │   └── resources/
│       │       ├── application.yml       # shared config
│       │       ├── application-dev.yml   # local Postgres, local storage, console email
│       │       ├── application-prod.yml  # RDS, S3, SES (values from env vars)
│       │       ├── db/migration/         # Flyway: V1__init.sql, V2__seed.sql ...
│       │       └── templates/email/      # otp.html, new-recipe.html, warning.html ...
│       └── test/java/com/cookify/        # unit + integration tests
│
├── frontend/                     # Plain HTML / CSS / JS (ES modules), no build step
│   ├── index.html
│   ├── pages/                    # login, signup, forgot-password, browse, recipe, upload, profile, settings, chat, admin
│   ├── css/
│   │   ├── tokens.css  base.css  components.css
│   │   └── pages/
│   ├── js/
│   │   ├── core/                 # api.js (fetch + token refresh), auth.js, i18n.js, socket.js, config.js
│   │   ├── components/           # header, footer, recipeCard, starRating, rangeSlider, toggle, toast, modal
│   │   └── pages/                # one module per page
│   ├── locales/                  # en.json, hi.json, zh.json, ru.json, es.json, fr.json
│   └── assets/{img,icons}/
│
├── infra/
│   ├── aws/                      # setup guide, IAM policies, S3 bucket policy, CloudFront config
│   ├── nginx/                    # cookify.conf (reverse proxy :80 → :8080, WebSocket upgrade)
│   ├── systemd/                  # cookify.service (runs the jar)
│   └── scripts/                  # deploy-backend.sh, deploy-frontend.sh
│
└── docs/
    ├── architecture.md
    └── screenshots/
```

**Why package-by-feature:** each feature (`recipe`, `social`, `chat`…) owns its controller, service, repository, entities and DTOs, so features stay isolated and can grow (or be split into services) without touching others.

---

## 5. Backend Approach

### Stack
| Concern | Choice |
|---|---|
| Language / framework | **Java 21 + Spring Boot 4** (Maven) |
| Web | Spring Web (REST) |
| Persistence | Spring Data JPA (Hibernate) + **PostgreSQL** |
| Migrations | **Flyway** |
| Security | Spring Security, BCrypt, JWT (Spring OAuth2 Resource Server, HS256), httpOnly refresh cookie |
| 2FA | Email OTP (6 digits, 5-min expiry, hashed in DB) |
| Validation | Jakarta Bean Validation (`@Valid`, `@NotBlank`, …) |
| File uploads | Multipart → **AWS S3** (local disk in dev) via AWS SDK v2 |
| Email | **AWS SES** via AWS SDK v2 (console notifier in dev) |
| Real-time chat | Spring WebSocket + **STOMP** (SockJS fallback) |
| Async work | `@Async` + `@EnableAsync` for follower emails |
| Rate limiting | Bucket4j on auth endpoints |
| API docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`) |
| Mapping / boilerplate | Lombok, MapStruct |
| Tests | JUnit 5, Mockito, Spring Boot Test, Testcontainers (Postgres) |

### Layering
`Controller (DTOs, validation) → Service (business logic, transactions) → Repository (JPA) → PostgreSQL`

### Database Schema
```
users(id, username UNIQUE, email UNIQUE, phone UNIQUE, password_hash, first_name, last_name,
      age, gender, avatar_url, bio, role[USER|ADMIN], twofa_enabled, warnings_count,
      banned_until, permanently_banned, preferred_lang, created_at)
user_preferences(user_id PK/FK, diet_type, max_calories, min_protein)
trusted_devices(id, user_id, token_hash, user_agent, expires_at)
otp_codes(id, user_id, code_hash, purpose[LOGIN|RESET], expires_at, used)
refresh_tokens(id, user_id, token_hash, expires_at, revoked)

recipes(id, author_id, title, description, steps JSONB, diet_type, cuisine, course,
        prep_minutes, cook_minutes, cost, calories, protein, difficulty, speed,
        views, avg_rating, rating_count, search_vector TSVECTOR, created_at, updated_at)
recipe_media(id, recipe_id, type[IMAGE|VIDEO], s3_key, url, position)
ingredients(id, name UNIQUE)          recipe_ingredients(recipe_id, ingredient_id, quantity)
equipment(id, name UNIQUE)            recipe_equipment(recipe_id, equipment_id)

ratings(id, recipe_id, user_id, score 1-5, UNIQUE(recipe_id, user_id))
comments(id, recipe_id, user_id, body, created_at)
saved_recipes(user_id, recipe_id)
follows(follower_id, followee_id, created_at)
warnings(id, user_id, reason, content_snippet, created_at)

conversations(id, created_at)
conversation_members(conversation_id, user_id, last_read_at)
messages(id, conversation_id, sender_id, body, created_at)
```
Indexes on filter columns (`diet_type`, `cost`, `calories`, `cook_minutes`, `avg_rating`, `views`) and a GIN index on `search_vector`.

### API Overview
| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/signup`, `/login`, `/verify-otp`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password` |
| Users | `GET/PATCH /api/users/me`, `POST /api/users/me/avatar`, `GET /api/users/{username}`, `POST/DELETE /api/users/{id}/follow`, `GET /api/users/me/devices` |
| Recipes | `GET /api/recipes` (search + filters + sort + paging), `GET /api/recipes/recommended`, `GET /api/recipes/{id}`, `POST`, `PUT`, `DELETE`, `POST /api/recipes/{id}/media`, `POST /api/recipes/{id}/save` |
| Social | `GET/POST /api/recipes/{id}/comments`, `DELETE /api/comments/{id}`, `PUT /api/recipes/{id}/rating` |
| Chat | `GET /api/chat/conversations`, `POST /api/chat/conversations`, `GET /api/chat/conversations/{id}/messages`; STOMP endpoint `/ws`, send `/app/chat.send`, subscribe `/user/queue/messages` |
| Admin | `GET /api/admin/users`, `POST /api/admin/users/{id}/ban`, `/unban`, `GET /api/admin/warnings` |
| Meta | `GET /api/meta/filters` (ingredients, equipment, cuisines, courses) |

### OOP Design
```java
// Recipe categorisation — inheritance + polymorphism
public abstract class Recipe {
    protected Long id; protected String title; protected int calories; ...
    public abstract DietType getDietType();
    public abstract String getDietBadge();                    // label + colour
    public abstract boolean isSuitableFor(DietPreference p);  // polymorphic dietary check
    public List<String> validate() { ...common rules... }     // subclasses extend (e.g. Jain: no onion/garlic)
}
class VegRecipe extends Recipe { ... }
class VeganRecipe extends VegRecipe { ... }      // vegan ⊂ veg
class JainRecipe extends VegRecipe { ... }
class EggetarianRecipe extends Recipe { ... }
class PescetarianRecipe extends Recipe { ... }
class NonVegRecipe extends Recipe { ... }
RecipeFactory.from(RecipeEntity e) → correct subclass

// Search — one class per filter (JPA Specifications)
public interface RecipeFilter { Specification<RecipeEntity> toSpecification(); boolean isActive(); }
IngredientFilter, EquipmentFilter, BudgetFilter, TimeFilter, NutritionFilter,
DietFilter, CuisineFilter, CourseFilter, DifficultyFilter, MinRatingFilter, TextSearchFilter
FilterFactory.from(SearchRequest) → List<RecipeFilter>  → combined with Specification.and()

// Sorting — strategy pattern
public interface SortStrategy { Sort toSort(); }
PopularitySort, TopRatedSort, NewestSort, QuickestSort

// Notifications
public abstract class Notifier { public abstract void send(User to, EmailTemplate t, Map<String,Object> data); }
SesEmailNotifier (prod), ConsoleNotifier (dev)

// Media storage
public interface StorageService { StoredFile upload(MultipartFile f, String folder); void delete(String key); }
S3StorageService (prod), LocalStorageService (dev)
```
Adding a diet, filter, sort or notifier = adding one class; the search service does not change.

### Key Flows
- **Login + 2FA:** verify password → if 2FA on and device not trusted → email OTP via SES → verify → issue access JWT (15 min) + refresh cookie (30 days, rotated). "Remember this device" sets a signed trusted-device cookie that skips OTP for 30 days.
- **Recommendations:** score = diet match + followed-author boost + rating + views, decayed by age; excludes the user's own recipes.
- **Popularity:** `score = avg_rating × ln(rating_count+1) + ln(views+1) − age_days × k`.
- **Comment moderation:** profanity check → blocked → `warnings` row, `warnings_count++`, warning email → at 3 warnings → ban (temporary or permanent), delete the user's comments, ratings and recipes, revoke tokens. A security filter rejects banned users.
- **Subscription notification:** on recipe create → `@Async` job emails all followers via SES (batched).

### Security
- BCrypt (strength 12); password rule ≥ 9 chars, no spaces.
- Access token held in memory on the client; refresh token in `httpOnly; Secure; SameSite=Lax` cookie.
- OTP and reset tokens stored hashed with expiry; Bucket4j rate limits on login, OTP and signup.
- Parameterised queries (JPA), upload type/size checks, output escaping on the frontend, CORS limited to the CloudFront domain.

---

## 6. Hosting & Deployment Approach (AWS)

### Architecture
```
                         ┌──────────────────────────── CloudFront (HTTPS, *.cloudfront.net) ───────────────────────────┐
 Browser ──────────────► │  /*        → S3 bucket  cookify-frontend   (static HTML/CSS/JS)                            │
                         │  /api/*    → EC2 (Nginx :80 → Spring Boot :8080)                                            │
                         │  /ws/*     → EC2 (WebSocket upgrade)                                                        │
                         └─────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                     ┌────────────────────────────┼────────────────────────────┐
                     ▼                            ▼                            ▼
          RDS PostgreSQL (private)      S3 bucket cookify-media        SES (email: OTP, notifications)
```
One CloudFront domain serves both frontend and API → same origin, so cookies and WebSockets work without CORS issues.

### Services (free tier)
| Component | AWS service | Free tier (first 12 months) |
|---|---|---|
| Backend API + chat | **EC2** t3.micro (Amazon Linux 2023, Java 21, Nginx, systemd) | 750 hrs/month |
| Database | **RDS PostgreSQL** db.t3.micro | 750 hrs/month, 20 GB |
| Frontend | **S3** static bucket (private, served via CloudFront OAC) | 5 GB |
| Media (images/videos) | **S3** `cookify-media` bucket, public read via CloudFront | 5 GB |
| CDN + HTTPS | **CloudFront** | 1 TB/month |
| Email | **SES** | 3,000 messages/month (sandbox: verified recipients only until production access approved) |
| Secrets | EC2 env file / SSM Parameter Store (standard) | Free |
| Logs | CloudWatch (basic) | Free tier |

### Access & Security
- EC2 has an **IAM role** with least-privilege access to S3 (`cookify-media`) and SES — no AWS keys in code or config.
- RDS in a private subnet; security group only allows port 5432 from the EC2 security group.
- EC2 security group: port 80 from CloudFront only, port 22 from your IP only.

### Environments
| Env | DB | Media | Email | Run |
|---|---|---|---|---|
| Local (`dev` profile) | Local Postgres | `uploads/` folder | Console | `./mvnw spring-boot:run` + VS Code Live Server for `frontend/` |
| Production (`prod` profile) | RDS | S3 | SES | `java -jar cookify.jar` under systemd |

### Environment Variables (prod)
```
SPRING_PROFILES_ACTIVE=prod
DB_URL, DB_USERNAME, DB_PASSWORD
JWT_SECRET (Base64, ≥ 256-bit)
AWS_REGION, S3_MEDIA_BUCKET, MEDIA_BASE_URL
SES_FROM_ADDRESS
APP_URL (CloudFront URL)
```

### Deployment Steps
1. **RDS:** create PostgreSQL db.t3.micro; note endpoint. Flyway creates tables on first app start.
2. **S3:** create `cookify-frontend` and `cookify-media` buckets.
3. **SES:** verify sender email (and test recipients while in sandbox).
4. **EC2:** launch t3.micro with IAM role → install Java 21 + Nginx → copy `infra/nginx/cookify.conf` and `infra/systemd/cookify.service` → set env file.
5. **Backend deploy:** `./mvnw package` → `scp` jar → `systemctl restart cookify` (`infra/scripts/deploy-backend.sh`).
6. **Frontend deploy:** `aws s3 sync frontend/ s3://cookify-frontend` → CloudFront invalidation (`infra/scripts/deploy-frontend.sh`).
7. **CloudFront:** distribution with three behaviours (`/*` → S3, `/api/*` and `/ws/*` → EC2 public DNS, caching disabled for API).
8. Smoke-test live URL against the PDF test table; capture screenshots.

### CI/CD (GitHub Actions)
- `ci.yml`: on push/PR → set up JDK 21 → `./mvnw verify` (tests).
- `deploy.yml`: on push to `main` → build jar → deploy to EC2 over SSH → sync frontend to S3 → invalidate CloudFront. AWS credentials via GitHub OIDC role.

### Cost Control
- Stay on free-tier instance types; set an **AWS Budget alert at $1**.
- Single EC2 + single-AZ RDS; stop RDS when not demoing if needed.

---

## 7. Deliverables
- Live URL (CloudFront).
- GitHub repository with README (setup, architecture, env vars, test accounts).
- `docs/screenshots/` — one per test case and main page.
- Seed data: demo users (incl. admin) and ~30 recipes across all diets/cuisines.

---

## 8. Risks & Mitigations
| Risk | Mitigation |
|---|---|
| AWS free tier expiry / surprise charges | Free-tier instance sizes only; $1 budget alert; tear-down script |
| SES sandbox (only verified recipients) | Verify demo accounts; request production access early |
| EC2 t3.micro has 1 GB RAM | Limit JVM heap (`-Xmx512m`); add 1 GB swap |
| Email deliverability / limits | SES with verified domain/sender; async sending; console fallback in dev |
| Video storage limits | Cap upload size (e.g. 50 MB) and duration |
| Profanity filter false positives | Configurable word list; admin can clear warnings |
| Time constraint | Chat and admin panel kept minimal-but-functional; i18n covers UI strings only (user content via Google Translate) |

---

## 9. Decisions
| Decision | Status |
|---|---|
| Backend stack: Java 21 + Spring Boot 4 + PostgreSQL | ✅ Decided |
| Hosting: AWS (EC2, RDS, S3, CloudFront, SES) | ✅ Decided |
| AWS account / region | ⏳ Pending |
| SES sender email | ⏳ Pending |
| GitHub account / repository name | ⏳ Pending |
| Custom domain (optional, else CloudFront URL) | ⏳ Pending |

---

## 10. Backend Build Steps & Verification

### Standard verification (every step)
| # | Check | How |
|---|---|---|
| 1 | Builds & starts | `./mvnw spring-boot:run` starts clean; Flyway migrations applied |
| 2 | Automated tests | `./mvnw test` — unit tests (JUnit 5 + Mockito) + integration tests (MockMvc on a test DB) all green |
| 3 | Manual API check | Swagger UI `http://localhost:8080/swagger-ui.html` **or** `backend/http/<step>.http` via VS Code REST Client |
| 4 | DB check | `psql cookify` — inspect the rows the step writes |

A step is **done** only when every checklist item below is ticked. Each finished step = one git commit.

### Frontend checkpoint A — auth UI (after Step 3)
- [x] Design system: tokens (mockup colours, Fraunces + Poppins), buttons, pill inputs, cards, toggle, OTP boxes, toast, modal
- [x] Core JS: API client (Bearer token + auto-refresh on 401, shared refresh promise), session helpers, i18n (`data-i18n`, English), form error mapping
- [x] Pages: landing, sign up (live password rules, field errors), login (+ OTP step, remember device), forgot / reset password, account (profile card, 2FA toggle with password, remembered devices, logout)
- [x] Protected pages redirect to login with `?next=`; guests don't probe the refresh endpoint
- [x] Backend: 30 s grace window for parallel refreshes (multiple tabs) — V4 `revoke_reason`
- [x] Browser test (headless Chrome): 18 flows pass, no JS errors; desktop + mobile screenshots checked

### Steps & checklists

#### Step 0 — Environment & skeleton
- [x] JDK 21 installed (`java -version`)
- [x] Local Postgres databases `cookify` (dev) and `cookify_test` (tests) exist
- [x] `./mvnw spring-boot:run` starts on port 8080
- [x] `GET /api/health` → `{"status":"UP", ...}` with `db: UP`
- [x] Swagger UI loads
- [x] `./mvnw test` → 4 tests green


#### Step 1 — Common foundation
- [x] Invalid request body → `400` with JSON `{ "error": { "code", "message", "fields" } }`
- [x] Malformed JSON / bad query param → `400`
- [x] Unknown route → `404` in the same JSON shape
- [x] Wrong HTTP method → `405`
- [x] Unhandled exception → `500` without stack trace or internal message leaking
- [x] Not logged in → `401` JSON (security layer uses same error shape)
- [x] CORS allows configured frontend origins only
- [x] Flyway baseline migration applied (`flyway_schema_history` v1, `citext` + `pg_trgm` extensions)
- [x] `./mvnw test` → 17 tests green

#### Step 2 — Auth core
- [x] Signup succeeds and logs the user in; duplicate username/email (case-insensitive) → `409`
- [x] Password < 9 chars, containing spaces or non-ASCII symbols → `400`; confirm password must match
- [x] Login by username, email or phone returns access token + sets httpOnly refresh cookie (`Path=/api/auth`)
- [x] Wrong password and unknown user → same `401 INVALID_CREDENTIALS`
- [x] `GET /api/users/me` without / invalid / expired token → `401`; with token → current user
- [x] Refresh rotates the refresh token; replaying an old one revokes all sessions
- [x] Logout revokes refresh token and clears cookie
- [x] Banned user → `403 ACCOUNT_BANNED`
- [x] `users.password_hash` is a BCrypt hash
- [x] `./mvnw test` → 43 tests green

#### Step 3 — 2FA, trusted devices, password reset
- [x] Enable/disable 2FA requires current password (`PUT /api/users/me/two-factor`)
- [x] With 2FA on, login returns `{ twoFactorRequired, challengeId, maskedEmail }`; 6-digit code printed in console email
- [x] `POST /api/auth/verify-otp` with correct code → tokens; code works only once
- [x] Wrong code → `401 INVALID_OTP` with attempts left; after 5 wrong / expired / superseded → `401 OTP_EXPIRED`
- [x] "Remember this device" → httpOnly `cookify_device` cookie; next login on same device skips OTP
- [x] Devices listed and revocable (`GET/DELETE /api/users/me/devices`); another user's device cookie doesn't skip
- [x] Forgot password → always `202`; reset link emailed (console) only if account exists
- [x] Reset sets new password, link is one-time, expires in 30 min, older links invalidated; all sessions and devices revoked
- [x] Branded HTML email templates (layout + login-otp + password-reset), user values HTML-escaped
- [x] `./mvnw test` → 67 tests green

#### Step 4 — User profile (backend + UI)
- [x] `PUT /api/users/me`: username, first/last name, phone, age (13–120), gender, bio (≤ 500), language (en/hi/zh/ru/es/fr); username/phone uniqueness → `409`
- [x] `GET/PUT /api/users/me/preferences`: diet type, max calories, min protein (ranges validated; null = no preference)
- [x] Avatar upload `POST /api/users/me/avatar` (≤ 5 MB): type detected from file bytes (JPEG/PNG/GIF/WebP), fake images → `400 INVALID_IMAGE`; old file deleted on replace; `DELETE` removes it
- [x] `StorageService` abstraction → `LocalStorageService` (dev, served at `/media/**`, path-traversal safe); S3 in Step 12
- [x] Public profile `GET /api/users/{username}` without login; hides email, phone, role, flags; banned/unknown → `404`
- [x] UI: account page with Profile / Preferences / Security tabs (URL hash), drag-and-drop avatar with client-side square crop, diet chips, calorie/protein sliders, bio counter
- [x] UI: public profile page (`/pages/profile.html?u=…`) with hero, facts, Edit (own) / Subscribe placeholder (others), not-found state
- [x] `./mvnw test` → 87 tests green; browser test 13/13 + auth regression 18/18, no JS errors

#### Step 5 — Recipe domain & CRUD (backend + UI)
- [x] V6: `recipes` (steps JSONB, cost INR, nutrition, difficulty, views, rating fields), shared `ingredients` / `equipment`, `recipe_media`
- [x] **OOP**: abstract `Recipe` → `VegRecipe` (→ `VeganRecipe`, `JainRecipe`), `EggetarianRecipe`, `PescetarianRecipe`, `NonVegRecipe`; `RecipeFactory`; polymorphic `getDietBadge()`, `isSuitableFor()`, `forbiddenIngredients()` (extended via `super`), shared `validate()` / `getSpeed()`
- [x] Diet rules on save: e.g. Jain + onion / Vegan + paneer → `400` on `ingredients`; plant look-alikes allowed (coconut milk, eggplant)
- [x] `RecipeFactory.dietsSuitableFor(pref)` derived from the subclasses (VEG → VEG, VEGAN, JAIN) — used by filters/recommendations
- [x] CRUD: create `201`, public view (+1 view unless author), update/delete by author or admin (`403 NOT_RECIPE_OWNER` otherwise)
- [x] Media: images ≤ 10 MB / videos ≤ 50 MB (MP4, MOV, WebM by magic bytes), max 10, first image = cover; files deleted with media/recipe
- [x] Lists: `GET /api/recipes` (latest), `GET /api/users/{username}/recipes`; profile shows recipe count
- [x] UI: editor (create/edit) with drag-and-drop media + previews, diet chips, cuisine/course, ₹ cost, star difficulty, ingredient rows, utensil tags with suggestions, re-orderable steps, upload progress
- [x] UI: recipe page (mockup layout: gallery with video, numbered method, facts, tickable ingredients, equipment, edit/delete), recipe cards on profile + landing, header "Upload recipe"
- [x] `./mvnw test` → 140 tests green; browser test 17/17, no JS errors

#### Step 6 — Search & filters (backend + UI)
- [x] V7: generated `total_minutes` + `search_vector` (English stemming) columns, GIN/trigram/filter indexes, `recipe_matches_text()` and `recipe_popularity()` SQL functions
- [x] **OOP filters**: abstract `RecipeFilter` → `TextSearchFilter`, `DietFilter`, `CuisineFilter`/`CourseFilter` (via generic `AnyOfFilter<T>`), `IngredientFilter` (ALL / ONLY-with-pantry-staples), `EquipmentFilter` (utensils I have), `BudgetFilter`, `TimeFilter`, `NutritionFilter`, `DifficultyFilter`, `MinRatingFilter`; `FilterFactory` builds active ones → JPA Specifications ANDed
- [x] **Sort strategies**: `PopularitySort` (rating × count + views − age), `TopRatedSort`, `NewestSort`, `QuickestSort`, `CheapestSort`
- [x] `GET /api/recipes` with all filters + sort + paging; invalid values → `400`; `GET /api/meta/filters` (choices, popular ingredients/equipment, slider ranges)
- [x] Demo data (dev only): 5 cooks + 39 recipes across all diets/cuisines/courses, loaded through `RecipeService` so diet rules validate it
- [x] UI: browse page — search bar, sort, veg-only toggle + diet dropdown, speed & difficulty stars (mockup), "what's in your kitchen" tags with autocomplete + ALL/ONLY, utensil chips, budget/calorie/protein sliders, rating/course/cuisine chips, removable filter pills, load more, URL-synced state, mobile drawer
- [x] Header "Browse" link, landing hero search
- [x] `./mvnw test` → 152 tests green; browser test 16/16, no JS errors

#### Step 7 — Social (backend + UI)
- [x] V8: `ratings` (unique per user+recipe), `comments`, `saved_recipes`, `follows`; `recipes.rating_sum` / `comment_count`
- [x] Rating 1–5; re-rating replaces score; average updated atomically in SQL (keeps demo totals); can't rate own recipe
- [x] Comments create / list (newest first, paged) / delete by comment author, recipe author or admin; count on cards
- [x] Save / unsave (idempotent); private saved list `GET /api/users/me/saved`
- [x] Subscribe / unsubscribe; follower & following counts; cannot follow self; viewer flags on recipe + profile
- [x] UI: recipe social strip (Subscribe, Save, 💬 count, avg stars, interactive "Your rating"), comment thread with delete + load more, login prompts for guests
- [x] UI: profile stats, working Subscribe, Uploaded + Saved carousels with ‹ › arrows (mockup)
- [x] `./mvnw test` → 161 tests green; browser test 16/16, no JS errors

#### Step 8 — Moderation (backend + UI)
- [x] `ProfanityFilter` (word list in `moderation/blocked-words.txt`): whole words, leetspeak, stretched letters, plurals; English + Hinglish
- [x] Abusive comment or recipe text → `400 CONTENT_BLOCKED` "warning N of 3"; warning row stored (own transaction) + email
- [x] 3rd warning → 30-day suspension (configurable); comments, ratings (averages restored), recipes + media, saves and follows removed; sessions + devices revoked; email
- [x] `BanCheckFilter`: banned user's still-valid access token → `403 ACCOUNT_BANNED`; login refused; profile hidden
- [x] Admin API (`ROLE_ADMIN`): list/search users (All / Warned / Banned), view warnings, ban N days or permanently with reason, unban (resets warnings); admins can't be banned
- [x] UI: blocked-comment message, auto-logout to "suspended" notice, admin moderation page (strikes, status chips, warnings dialog, ban dialog, unban), Admin link for admins
- [x] Demo admin account `admin` / `Cookify2026!`
- [x] `./mvnw test` → 186 tests green; browser test 13/13, no JS errors

#### Step 9 — Notifications
- [x] Services publish domain events (`RecipePublished`, `CommentAdded`, `RecipeRated`, `UserFollowed`); `ActivityNotifier` handles them **after commit** on a background thread pool (`@Async` + `@TransactionalEventListener`)
- [x] New recipe → every (non-banned) subscriber gets an email; request not blocked (recipe created in ~120 ms)
- [x] Owner notified on new comment (not own), first rating (not re-ratings), new subscriber (not repeats)
- [x] Branded HTML templates: new-recipe, new-comment, new-rating, new-follower (+ warning / banned from Step 8); links built from `app.frontend-url`
- [x] Blocked / rolled-back actions send nothing; email failures are logged, never break the action
- [x] `./mvnw test` → 191 tests green (sync executor in tests); live check: 4/4 email types

#### Step 10 — Recommendations (backend + UI)
- [x] `GET /api/users/me/recommendations`: hard filter = diets from `RecipeFactory.dietsSuitableFor(preference)`; own and already-rated recipes excluded
- [x] Score = trending (`recipe_popularity`) + follow boost (20) + liked-cuisine boost (8, from saved / rated ≥ 4) + calorie / protein target boosts (4 each)
- [x] Each item carries reasons (FOLLOWED_AUTHOR, LIKED_CUISINE, FITS_DIET, FITS_CALORIES, FITS_PROTEIN, TRENDING)
- [x] Veg user sees only Veg / Vegan / Jain; Jain user only Jain; followed cook's recipes on top
- [x] UI: "✨ Picked for you" carousel on the landing page with a reason line on each card; link to tune preferences
- [x] `./mvnw test` → 199 tests green; browser check passed, no JS errors

#### Step 11 — Chat (backend + UI)
- [x] V10: `conversations`, `conversation_members` (last read), `messages`
- [x] REST: start / list (unread counts) / history with cursor paging / send / mark read / total unread
- [x] WebSocket STOMP on `/ws`: JWT in CONNECT frame (`StompAuthInterceptor`), banned users refused, subscribe only to own `/user/queue/*`; live messages + typing indicator
- [x] Outsiders get `404`; messages screened by the profanity filter (warning reason `ABUSIVE_MESSAGE`)
- [x] UI: two-pane chat page (inbox + thread, day dividers, load older, Enter to send, typing hint, live status, mobile single-pane), header 💬 unread badge, "Message" button on profiles
- [x] Two users in two browsers exchange messages in real time; history persists after reload
- [x] Profanity list extended with Hinglish short forms (bsdk, bkl, …)
- [x] `./mvnw test` → 210 tests green (incl. real WebSocket client tests); browser test passed, no JS errors

#### Multi-language (UI)
- [x] Locale files for English, Hindi, Mandarin, Russian, Spanish, French (≈390 strings each; admin page and dev hints fall back to English)
- [x] Header 🌐 language menu; choice saved per device and to the user's profile (`preferredLang`), applied automatically on a new device after login
- [x] First visit follows the browser language; `<html lang>` always set, so the browser's own translation (Google Translate) also covers user-written recipes and comments
- [x] Dates, relative times and counts formatted in the selected language
- [x] Validation script: no missing keys, `{placeholders}` identical across languages; browser test across all 6 languages, no JS errors

#### Step 12 — AWS integrations
Code (done):
- [x] `S3StorageService` (objects under `media/`, served by CloudFront `/media/*`, year-long caching) and `SesEmailNotifier` (HTML + text, failures logged) behind config switches; AWS SDK v2, credentials from the EC2 instance role
- [x] `prod` profile fully driven by environment variables (`infra/aws/cookify.env.example`), demo data optional
- [x] Infra: Nginx site (frontend + `/api` + WebSocket `/ws`, spoof-proof client IP), systemd unit, `setup-server.sh`, `deploy.sh`, IAM + bucket policies, step-by-step guide `infra/aws/README.md`
- [x] Tests: mocked S3/SES clients + wiring test for the AWS profile → 219 tests green

Deployment (together):
- [ ] `prod` profile: avatar/recipe media stored in S3 and served via URL
- [ ] OTP email arrives in a real inbox via SES
- [ ] App connects to RDS; Flyway migrations applied there
- [ ] Live URL in README; screenshots in `docs/screenshots/`

#### Step 13 — Hardening & tests
- [x] Sliding-window rate limits (`RateLimitFilter`): per IP on login, 2FA code, signup, forgot/reset password, refresh; per user on comments, chat messages, ratings, recipe/media uploads → `429 TOO_MANY_REQUESTS` + `Retry-After` (on by default, off in `dev`)
- [x] 10 rapid login attempts → `429`; other clients unaffected; window frees up afterwards
- [x] Security headers: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`, HSTS on HTTPS
- [x] Production trusts `X-Forwarded-*` (real client IP behind Nginx / CloudFront)
- [x] Full suite: `./mvnw test` → 214 tests green; Swagger documents every endpoint; browser regressions (auth 18/18, chat, i18n) pass
