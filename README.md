# Flicktion
Choose films with your friends to watch

## Local backend development

Start the local PostgreSQL and Mailpit services from the repository root:

```powershell
docker compose up -d postgres mailpit
```

The `local` Spring profile reads the repository `.env` file (or environment
variables), connects to those services on `localhost`, and routes application
email to Mailpit at http://localhost:8025. Google OAuth uses Google's token
and user-info endpoints directly, with
`http://localhost:8080/auth/google/callback` as the default callback. To test
Google sign-in, set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and (if needed)
`GOOGLE_OAUTH_REDIRECT_URI` in `.env`, and register the callback URL in Google
Cloud Console. Start the backend with:

```powershell
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

Integration tests use Testcontainers and therefore require Docker:

```powershell
mvn -f backend/pom.xml test
```

## Account moderation API

Users can submit account reports with `POST /moderation/reports`, using
`reportedAccountId`, `category` (`HARASSMENT`, `IMPERSONATION`, `SPAM`,
`COPYRIGHT`, or `OTHER`), and `details`. Administrators assign the
`MODERATOR` access level through the existing account access-level endpoint.
Moderators can review pending reports with `GET /moderation/reports`, record
a `CONFIRMED` or `DISMISSED` decision with
`POST /moderation/reports/{reportId}/decision` (`note` is required), and
inspect the decision history with `GET /moderation/decisions`. A confirmed
decision creates a warning by default; it may instead include
`sanction: "WARNING"` or `sanction: "TIMEOUT"`. Timeouts require
`durationHours` between 1 and 168.
Warnings are recorded without restricting access. A timeout blocks login and
requests made with existing access tokens until it expires.

Administrators can inspect paginated account statuses with
`GET /moderation/accounts`, permanently ban an account with
`POST /moderation/accounts/{accountId}/ban`, or lift an active timeout/ban with
`POST /moderation/accounts/{accountId}/lift`; both actions require a `note`.
Account details are available at `GET /moderation/accounts/{accountId}` and
their full action history at `GET /moderation/accounts/{accountId}/actions`.
Every warning, timeout, ban, and lift is retained in that history; the admin
account list shows the current status and latest reason.
This module currently restricts account access; action-specific restrictions
for projects and audio revisions can be added when those features exist.
