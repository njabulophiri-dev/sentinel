# Deploying Sentinel

Sentinel deploys as **one container** (the API and the dashboard together) plus **one PostgreSQL database**.
Any host that runs Docker images and offers PostgreSQL will work.

> Hosting plans, free tiers and prices change often. Check the host's current pricing and limits before you
> commit to one. Nothing here has been tested on a live host yet, so treat the first deployment as a test.

## Before you deploy

- [ ] CI is green on `main` (backend, frontend and image jobs).
- [ ] You have read `docs/adr/008` and `009`.
- [ ] You will use the `prod` profile, never `dev`. The `dev` profile has public passwords.
- [ ] You have generated your own secrets (next section).

## Generate your secrets

```
openssl rand -base64 48     # SENTINEL_JWT_SECRET: at least 32 bytes
openssl rand -base64 24     # a password for the analyst demo account
openssl rand -base64 24     # SENTINEL_ADMIN_PASSWORD
openssl rand -base64 24     # SENTINEL_SERVICE_PASSWORD
```

Keep them in a password manager. Never put them in Git, in a screenshot, or in a video.

## Environment variables

| Variable | Required | Meaning |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | Set to `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | yes | Database location (or give `DB_URL` as a full JDBC URL) |
| `DB_USER`, `DB_PASSWORD` | yes | Database login. The app will not start without the password |
| `SENTINEL_JWT_SECRET` | yes | Token signing secret, at least 32 bytes |
| `SENTINEL_ADMIN_PASSWORD` | yes | Creates the `admin` user on first start |
| `SENTINEL_SERVICE_PASSWORD` | yes | Creates the `service` user on first start |
| `SENTINEL_ANALYST_PASSWORD` | yes | Creates the `analyst` user on first start |
| `SENTINEL_DEMO_SEED` | no | `true` loads sample data into an empty database |
| `PORT` | no | Set by most hosts. The app listens on it |
| `JAVA_TOOL_OPTIONS` | no | For small hosts: `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k` |

Users are created only if they do not exist yet. Changing a password variable later does not change an existing user.

## Option A: Render, using the blueprint in the repo

`render.yaml` describes the service and database.

1. Push your work to GitHub and make sure `main` is green.
2. In Render, choose **New**, then **Blueprint**, and select the `sentinel` repository.
3. When asked, enter `SENTINEL_ANALYST_PASSWORD`. The other secrets are generated for you.
4. Wait for the build. The first one takes several minutes.
5. Open the service URL and run the smoke test (below).

Things to know about free plans on any host, which you should confirm on the current pricing page:
- Free web services may be put to sleep after a period with no traffic, and the first request after that can take
  a minute (the JVM also needs several seconds to start). That is a poor first impression for a recruiter. A small
  paid always-on instance is a few dollars a month on most hosts.
- Free databases may be small and may expire. Do not keep anything you care about in one.

## Option B: Railway, Fly.io or another host

Create a service from the repository's `Dockerfile` and a PostgreSQL database, then set the variables in the table
above. If the host gives you a single `postgres://` URL, split it into host, port, name, user and password, or build
the JDBC form `jdbc:postgresql://HOST:PORT/NAME` and set `DB_URL`. Use the host's health check path `/actuator/health`.

## Check the deployment

```
DEMO_USER=analyst DEMO_PASS='your analyst password' ./scripts/smoke.sh https://your-app.example.com
```

It checks health, that the dashboard is served, that the API refuses requests with no token, that login works, and
that metrics are not public. Then open the URL in a browser, sign in, and open an alert.

Also check that the login throttle sees real visitor addresses. Try six wrong passwords for a made-up username
from your phone. The sixth should answer `429`. In the logs, the warning should show your address, not an internal one.

## Put it on your CV and LinkedIn

- Link to the live URL and the repository, side by side.
- Publish only the analyst demo credentials, in the README, with a line saying the data is sample data.
- Say what it is plainly: a fraud detection API with a dashboard, tested end to end, deployed in one container.
- Mention what is not built (see the roadmap). Interviewers respect honest scope.

## If something goes wrong

| Symptom | Likely cause |
|---|---|
| App exits at start with "jwt-secret must be at least 32 bytes" | `SENTINEL_JWT_SECRET` is missing or too short |
| App exits at start with a "Could not resolve placeholder" error | A required variable, usually `DB_PASSWORD`, is not set |
| App exits with a connection error | Wrong `DB_HOST`, `DB_PORT`, `DB_NAME`, or the app and database are in different regions |
| Out of memory, or the host kills the container | Set `JAVA_TOOL_OPTIONS` as above, or use a larger instance |
| Dashboard loads but login fails for everyone | The user was created with an older password, or the throttle is blocking the shared proxy address |
| No alerts in the dashboard | `SENTINEL_DEMO_SEED` is not `true`, or the database already had transactions |
