# Deploying (Docker + Render)

Vercel cannot run Spring Boot. This project ships a `Dockerfile` and `render.yaml` so it can run on Render (or Railway / Fly.io / any Docker host).

## 1. Test the image locally (optional, needs Docker Desktop)
```bash
docker compose up --build        # app + MySQL, open http://localhost:8080
```
Or H2 only:
```bash
docker build -t supermarket .
docker run -p 8080:8080 supermarket
```

## 2. Push to GitHub
```bash
git init
git add .
git commit -m "supermarket billing"
git branch -M main
git remote add origin https://github.com/<you>/supermarket-billing.git
git push -u origin main
```

## 3. Deploy on Render
1. render.com > sign in with GitHub > **New + > Blueprint** > select the repo (it reads `render.yaml`).
   Or **New + > Web Service** > the repo > Runtime **Docker** > Free plan > Health Check Path `/login`.
2. Wait for the build (first build takes several minutes). You get `https://<name>.onrender.com`.
3. Login: `admin / admin123`, `cashier / cashier123`. **Change these passwords** before sharing the link.

## Data persistence
| Mode | Behaviour |
|------|-----------|
| `APP_PROFILE=h2` (default) | Demo only. Free Render has no disk, so data resets on every restart/redeploy (demo data is re-seeded automatically). |
| `APP_PROFILE=mysql` | Persistent. Use a hosted MySQL (Aiven free tier, Railway, TiDB Cloud) and set `DB_URL`, `DB_USER`, `DB_PASSWORD` in Render > Environment. |

MySQL example:
```
APP_PROFILE=mysql
DB_URL=jdbc:mysql://<host>:<port>/<db>?useSSL=true&serverTimezone=UTC
DB_USER=<user>
DB_PASSWORD=<password>
```
Uploaded logos live in `/app/uploads` and are also lost on free Render restarts.

## Notes
* Free Render services sleep after ~15 min idle; the first request afterwards takes ~30-60 s.
* The camera scanner works on the Render URL because it is HTTPS.
* Memory is tight on the free 512 MB plan; `JAVA_OPTS` is already tuned for it.
