FROM python:3.11-slim-bookworm

# Install UV
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    libatomic1 \
    openssl \
    && rm -rf /var/lib/apt/lists/*


# Copy configuration files
COPY pyproject.toml uv.lock ./

# Install dependencies
RUN uv sync --frozen --no-cache

# Copy the rest of the application
COPY . .



# Expose port
EXPOSE 8000

# Command to run the application with hot-reloading
CMD sh -c "uv run alembic upgrade head && uv run python start.py"
