# 🚀 Rupaya - Complete Developer Guide

> **Modern Bill-Splitting & Expense Tracking Application**  
> Built with FastAPI, Next.js, PostgreSQL, and Redis

---

## 📑 Table of Contents

- [Quick Start](#-quick-start)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Local Development](#-local-development)
- [Database Management](#-database-management)
- [API Documentation](#-api-documentation)
- [Production Deployment](#-production-deployment)
- [Environment Variables](#-environment-variables)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)

---

## ⚡ Quick Start

### Prerequisites
- [Docker](https://www.docker.com/get-started) & Docker Compose
- [Git](https://git-scm.com/)

### Run Locally (3 Commands)

```bash
# 1. Clone the repository
git clone https://github.com/your-username/Rupaya.git
cd Rupaya

# 2. Set up backend environment
cd backend && cp .env.example .env && cd ..

# 3. Start all services
docker compose up
```

**That's it!** 🎉 Access the app:
- 🎨 **Frontend**: http://localhost:3000
- 🔧 **Backend API**: http://localhost:8000
- 📚 **API Docs**: http://localhost:8000/docs
- 🗄️ **pgAdmin**: http://localhost:5050 (admin@admin.com / admin)
- ✅ **Health Check**: http://localhost:8000/health

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose | Version |
|------------|---------|---------|
| **FastAPI** | Web framework | Latest |
| **PostgreSQL** | Database | 16 |
| **SQLAlchemy** | ORM (Async) | Latest |
| **Alembic** | Migrations | Latest |
| **Redis** | Cache & sessions | 7.2 |
| **python-jose** | JWT auth | Latest |
| **UV** | Package manager | Latest |

### Frontend
| Technology | Purpose | Version |
|------------|---------|---------|
| **Next.js** | React framework | 16 |
| **TypeScript** | Type safety | Latest |
| **Tailwind CSS** | Styling | 4 |
| **Bun** | Runtime | Latest |
| **Framer Motion** | Animations | Latest |
| **Lucide React** | Icons | Latest |

### DevOps
- **Docker** & **Docker Compose** - Containerization
- **Render** - Backend hosting
- **Vercel** - Frontend hosting
- **pgAdmin** - Database management

---

## 📁 Project Structure

```
Rupaya/
├── backend/                    # FastAPI Backend
│   ├── app/
│   │   ├── core/              # Config, security, exceptions
│   │   ├── db/                # Database models & session
│   │   ├── models/            # Pydantic schemas
│   │   ├── routers/           # API endpoints
│   │   │   ├── auth.py       # Authentication
│   │   │   ├── users.py      # User management
│   │   │   ├── groups.py     # Group operations
│   │   │   ├── bills.py      # Bill management
│   │   │   └── summary.py    # Financial summaries
│   │   ├── services/          # Business logic
│   │   └── main.py            # FastAPI app
│   ├── alembic/               # Database migrations
│   ├── Dockerfile             # Local development
│   ├── Dockerfile.prod        # Production (Render)
│   ├── pyproject.toml         # Dependencies
│   └── start.py               # Entry point
│
├── frontend/                   # Next.js Frontend
│   ├── src/
│   │   ├── app/               # Next.js App Router
│   │   │   ├── (auth)/       # Login, Register
│   │   │   ├── (dashboard)/  # Dashboard, Groups
│   │   │   └── layout.tsx    # Root layout
│   │   ├── components/        # React components
│   │   │   └── ui/           # Reusable UI
│   │   ├── lib/              # API client, utilities
│   │   └── types/            # TypeScript types
│   ├── public/               # Static assets
│   ├── Dockerfile            # Local development
│   ├── vercel.json          # Production (Vercel)
│   └── package.json         # Dependencies
│
├── docker-compose.yml         # Local orchestration
└── Documentation files        # Guides (this file!)
```

---

## 🏠 Local Development

### Option 1: Full Docker (Recommended)

**Best for**: Complete isolation, running all services together

```bash
# Start all services
docker compose up

# Run in background
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Rebuild after code changes
docker compose up --build

# Remove everything including data
docker compose down -v
```

### Option 2: Hybrid Setup

**Best for**: Frontend development (faster hot reload)

```bash
# Terminal 1: Start backend services only
docker compose up postgres redis backend

# Terminal 2: Run frontend natively
cd frontend
bun install
bun dev
```

### Option 3: Fully Native

**Best for**: Maximum control, debugging

**Requirements**: PostgreSQL and Redis installed locally

```bash
# Terminal 1: Backend
cd backend
uv sync                        # Install dependencies
uv run alembic upgrade head    # Run migrations
uv run python start.py         # Start server

# Terminal 2: Frontend
cd frontend
bun install                    # Install dependencies
bun dev                        # Start dev server
```

### Common Development Commands

#### Docker Compose
```bash
# Start specific services
docker compose up postgres redis backend

# Restart a service
docker compose restart backend

# View service logs
docker compose logs -f backend

# Execute command in container
docker compose exec backend uv run alembic upgrade head

# Check running services
docker compose ps
```

#### Backend (Native)
```bash
cd backend

# Install dependencies
uv sync

# Run migrations
uv run alembic upgrade head

# Create new migration
uv run alembic revision --autogenerate -m "description"

# Start dev server
uv run python start.py

# Run tests
uv run pytest

# Format code
uv run ruff format .

# Lint code
uv run ruff check .
```

#### Frontend (Native)
```bash
cd frontend

# Install dependencies
bun install

# Start dev server
bun dev

# Build for production
bun run build

# Start production server
bun start

# Run linter
bun run lint

# Type check
bun run type-check
```

---

## 🗄️ Database Management

### Migrations with Alembic

```bash
cd backend

# Create new migration (after changing models)
uv run alembic revision --autogenerate -m "add user profile"

# Apply all pending migrations
uv run alembic upgrade head

# Rollback last migration
uv run alembic downgrade -1

# View migration history
uv run alembic history

# View current version
uv run alembic current
```

**Note**: Migrations run automatically when starting the backend container.

### Using pgAdmin

1. Open http://localhost:5050
2. Login: `admin@admin.com` / `admin`
3. Add server:
   - **Name**: Rupaya Local
   - **Host**: `postgres` (Docker) or `localhost` (native)
   - **Port**: `5432`
   - **Username**: `postgres`
   - **Password**: `postgres`
   - **Database**: `rupaya`

### Using psql (Command Line)

```bash
# Connect via Docker
docker compose exec postgres psql -U postgres -d rupaya

# Connect natively
psql -U postgres -d rupaya

# Common commands
\dt              # List tables
\d table_name    # Describe table
\q               # Quit
```

---

## 📚 API Documentation

### Endpoints Overview

#### Authentication (`/api/v1/auth`)
- `POST /register` - Register new user
- `POST /login` - Login user
- `POST /refresh` - Refresh access token

#### Users (`/api/v1/users`)
- `GET /me` - Get current user
- `GET /search` - Search users by email/username

#### Groups (`/api/v1/groups`)
- `GET /` - List user's groups (with pagination, search, filters)
- `POST /` - Create new group
- `GET /{id}` - Get group details
- `PATCH /{id}` - Update group
- `DELETE /{id}` - Delete group
- `POST /{id}/members` - Add member to group
- `DELETE /{id}/members/{user_id}` - Remove member
- `PATCH /{id}/members/{user_id}` - Update member role

#### Bills (`/api/v1/bills`)
- `GET /` - List user's bills (paginated)
- `POST /` - Create new bill
- `GET /group/{group_id}` - Get group bills (paginated, searchable)
- `GET /{id}` - Get bill details
- `PATCH /{id}` - Update bill
- `PATCH /shares/{share_id}/mark-paid` - Mark share as paid
- `PATCH /shares/{share_id}/mark-unpaid` - Mark share as unpaid

#### Summary (`/api/v1/summary`)
- `GET /` - Get user summary (global or per-group)

### Interactive Documentation

Once running, access:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

---

## 🚀 Production Deployment

### Architecture

```
┌─────────────────────────────────────────────────┐
│              PRODUCTION SETUP                    │
├─────────────────────────────────────────────────┤
│                                                  │
│  ┌──────────────┐         ┌──────────────┐     │
│  │   FRONTEND   │         │   BACKEND    │     │
│  │   Next.js    │────────▶│   FastAPI    │     │
│  │   Vercel     │  API    │   Render     │     │
│  └──────────────┘         └──────┬───────┘     │
│                                   │              │
│                          ┌────────┴────────┐    │
│                          │                  │    │
│                    ┌─────▼─────┐   ┌───────▼──┐ │
│                    │PostgreSQL │   │  Redis   │ │
│                    │  Render   │   │  Render  │ │
│                    └───────────┘   └──────────┘ │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Backend → Render

**Files Used**: `backend/Dockerfile.prod`, `backend/render.yaml`

#### Steps:

1. **Create Web Service** on [Render](https://dashboard.render.com)
   - New → Web Service
   - Connect GitHub repository
   - Select `Rupaya` repo

2. **Configure Service**:
   - **Name**: `rupaya-backend`
   - **Root Directory**: `backend`
   - **Runtime**: Docker
   - **Dockerfile**: `./Dockerfile.prod`
   - **Instance Type**: Free (or paid)

3. **Add PostgreSQL**:
   - New → PostgreSQL
   - **Name**: `rupaya-db`
   - Copy **Internal Database URL**

4. **Add Redis**:
   - New → Redis
   - **Name**: `rupaya-redis`
   - Copy **Internal Redis URL**

5. **Environment Variables**:
   ```env
   DATABASE_URL=<paste Internal Database URL>
   REDIS_URL=<paste Internal Redis URL>
   SECRET_KEY=<click Generate>
   JWT_ALGORITHM=HS256
   ACCESS_TOKEN_EXPIRE_MINUTES=30
   REFRESH_TOKEN_EXPIRE_DAYS=7
   ALLOWED_ORIGINS=https://your-app.vercel.app
   HOST=0.0.0.0
   PORT=8000
   PYTHONUNBUFFERED=1
   ```

6. **Deploy** → Copy backend URL (e.g., `https://rupaya-backend.onrender.com`)

### Frontend → Vercel

**Files Used**: `frontend/vercel.json`

#### Steps:

1. **Import Project** on [Vercel](https://vercel.com/dashboard)
   - Add New → Project
   - Import from GitHub

2. **Configure**:
   - **Framework**: Next.js (auto-detected)
   - **Root Directory**: `frontend`
   - **Build Command**: `bun run build`
   - **Install Command**: `bun install`

3. **Environment Variables**:
   ```env
   NEXT_PUBLIC_API_URL=https://rupaya-backend.onrender.com/api/v1
   NEXT_TELEMETRY_DISABLED=1
   ```
   ⚠️ Replace with YOUR backend URL from step 6 above

4. **Deploy** → Copy frontend URL (e.g., `https://rupaya.vercel.app`)

### Update CORS

After deploying frontend:
1. Go to Render → Your Web Service → Environment
2. Update `ALLOWED_ORIGINS` to your Vercel URL
3. Save (auto-redeploys)

### Verify Deployment

```bash
# Backend health check
curl https://rupaya-backend.onrender.com/health
# Should return: {"status":"healthy","service":"rupaya-api"}

# Frontend
# Visit your Vercel URL and test login/register
```

### Cost Breakdown

#### Free Tier
- Render: 750 hours/month + PostgreSQL + Redis
- Vercel: Unlimited deployments, 100GB bandwidth
- **Total**: $0/month

**Limitations**: Cold starts, no backups, limited resources

#### Paid Tier (Production)
- Render Web Service: $7/month
- Render PostgreSQL: $7/month
- Render Redis: $5/month
- Vercel Pro: $20/month
- **Total**: ~$39/month

**Benefits**: No cold starts, backups, better performance, custom domains

---

## 🔐 Environment Variables

### Backend (`.env`)

**Local Development** (`backend/.env`):
```env
# Server
HOST=0.0.0.0
PORT=8000

# Database (Docker)
DATABASE_URL=postgresql://postgres:postgres@postgres:5432/rupaya?schema=public

# Database (Native)
# DATABASE_URL=postgresql://postgres:postgres@localhost:5432/rupaya?schema=public

# Cache
REDIS_URL=redis://redis:6379/0

# Security
SECRET_KEY=super_secret_dev_key
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7

# CORS (development allows all)
# ALLOWED_ORIGINS=*
```

**Production** (Render Dashboard):
```env
DATABASE_URL=<from Render PostgreSQL>
REDIS_URL=<from Render Redis>
SECRET_KEY=<auto-generated>
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7
ALLOWED_ORIGINS=https://your-app.vercel.app
HOST=0.0.0.0
PORT=8000
PYTHONUNBUFFERED=1
```

### Frontend

**Local Development** (`frontend/.env.local`):
```env
NEXT_PUBLIC_API_URL=http://localhost:8000/api/v1
NEXT_TELEMETRY_DISABLED=1
```

**Production** (Vercel Dashboard):
```env
NEXT_PUBLIC_API_URL=https://rupaya-backend.onrender.com/api/v1
NEXT_TELEMETRY_DISABLED=1
```

⚠️ **Important**: Client-side variables MUST be prefixed with `NEXT_PUBLIC_`

---

## 🔍 Troubleshooting

### Port Already in Use

**Problem**: Port 3000, 8000, 5432, or 6379 already in use

**Solution**:
```bash
# Windows: Find process
netstat -ano | findstr :3000

# Windows: Kill process
taskkill /PID <process_id> /F

# Or change port in docker-compose.yml
ports:
  - "3001:3000"  # Use port 3001 instead
```

### Database Connection Failed

**Problem**: Backend can't connect to database

**Solution**:
```bash
# Check if PostgreSQL is running
docker compose ps

# Restart PostgreSQL
docker compose restart postgres

# View logs
docker compose logs postgres

# Verify DATABASE_URL in .env
```

### Frontend Can't Connect to Backend

**Problem**: API calls fail with network error

**Solution**:
1. Check backend is running: http://localhost:8000/health
2. Verify `NEXT_PUBLIC_API_URL` in `frontend/.env.local`
3. Check browser console for CORS errors
4. Ensure backend CORS allows `http://localhost:3000`

### Hot Reload Not Working

**Problem**: Changes don't reflect automatically

**Solution**:

**Backend**:
```bash
# Restart backend service
docker compose restart backend

# Or run natively for better hot reload
cd backend
uv run python start.py
```

**Frontend**:
```bash
# Run natively for best hot reload
cd frontend
bun dev
```

### Docker Build Fails

**Problem**: `docker compose up --build` fails

**Solution**:
```bash
# Clear Docker cache
docker compose down
docker system prune -a

# Rebuild from scratch
docker compose up --build
```

### Migration Errors

**Problem**: Alembic migration fails

**Solution**:
```bash
# Check current version
cd backend
uv run alembic current

# View migration history
uv run alembic history

# Rollback and retry
uv run alembic downgrade -1
uv run alembic upgrade head

# If stuck, reset database (⚠️ deletes data)
docker compose down -v
docker compose up
```

### CORS Errors in Production

**Problem**: Frontend can't connect to backend in production

**Solution**:
1. Go to Render → Web Service → Environment
2. Update `ALLOWED_ORIGINS` to your Vercel URL
3. Format: `https://your-app.vercel.app` (no trailing slash)
4. Save and wait for redeploy

---

## 🧪 Testing

### Backend Tests

```bash
cd backend

# Run all tests
uv run pytest

# Run with coverage
uv run pytest --cov=app --cov-report=html

# Run specific test file
uv run pytest app/tests/test_auth.py

# Run with verbose output
uv run pytest -v
```

### Frontend Tests

```bash
cd frontend

# Type checking
bun run type-check

# Linting
bun run lint

# Build test (catches build errors)
bun run build
```

---

## 🎯 Development Workflow

### Daily Workflow

```bash
# Morning: Start services
docker compose up -d

# Work on code...
# Changes auto-reload

# View logs if needed
docker compose logs -f backend

# Evening: Stop services
docker compose down
```

### Making Database Changes

```bash
# 1. Edit models in backend/app/db/models.py

# 2. Create migration
cd backend
uv run alembic revision --autogenerate -m "add user profile field"

# 3. Review migration file in backend/alembic/versions/

# 4. Apply migration
uv run alembic upgrade head

# 5. Restart backend
docker compose restart backend
```

### Deploying Changes

```bash
# 1. Commit and push
git add .
git commit -m "Add new feature"
git push origin main

# 2. Automatic deployment
# - Render auto-deploys backend
# - Vercel auto-deploys frontend

# 3. Monitor deployment
# - Check Render dashboard for backend logs
# - Check Vercel dashboard for frontend logs
```

---

## 🌟 Features

- ✅ **User Management**: Secure registration and authentication with JWT
- ✅ **Group Creation**: Create and manage expense groups
- ✅ **Bill Tracking**: Add bills and track who has paid
- ✅ **Expense Splitting**: Equal, exact, and percentage splits
- ✅ **Financial Summaries**: View balances and settlements
- ✅ **Search**: Find users to add to groups
- ✅ **Responsive Design**: Mobile-friendly interface
- ✅ **Real-time Updates**: (Planned with WebSockets)

---

## 🗺️ Roadmap

- [ ] Real-time expense updates using WebSockets
- [ ] Mobile application (React Native)
- [ ] Email notifications
- [ ] Export to CSV/PDF
- [ ] Multi-currency support
- [ ] Receipt image upload and OCR
- [ ] Settlement suggestions and optimization
- [ ] Payment integration (Stripe, PayPal)
- [ ] Group chat functionality
- [ ] Expense categories and tags

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Style

- **Backend**: Follow PEP 8, use `ruff` for formatting
- **Frontend**: Follow Airbnb style guide, use ESLint
- **Commits**: Use conventional commits format

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/your-username/Rupaya/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/Rupaya/discussions)
- **Email**: your-email@example.com

---

## 🙏 Acknowledgments

- FastAPI for the amazing web framework
- Next.js team for the powerful React framework
- Render and Vercel for excellent hosting platforms
- All contributors and users of Rupaya

---

## 📖 Additional Resources

- **Render Documentation**: https://render.com/docs
- **Vercel Documentation**: https://vercel.com/docs
- **FastAPI Documentation**: https://fastapi.tiangolo.com
- **Next.js Documentation**: https://nextjs.org/docs

---

**Made with ❤️ by the Rupaya Team**

🚀 **Ready to split bills like a pro!**
