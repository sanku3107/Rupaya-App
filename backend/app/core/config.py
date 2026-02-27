from datetime import timedelta

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # === Environment-based values ===
    SECRET_KEY: str = Field("super_secret_key", env="SECRET_KEY")
    DATABASE_URL: str = Field(..., env="DATABASE_URL")
    REDIS_URL: str = Field(..., env="REDIS_URL")
    PORT: int = Field(8000, env="PORT")
    HOST: str = Field("0.0.0.0", env="HOST")

    # === App constants ===
    api_base_path: str = "/api/v1"
    access_token_expire_minutes: int = 60
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE: timedelta = timedelta(hours=1)
    REFRESH_TOKEN_EXPIRE: timedelta = timedelta(days=7)

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        from_attributes = True


settings = Settings()
